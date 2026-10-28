package com.shottrack.backend.application.habituality.usecase;

import com.shottrack.backend.application.habituality.dto.HabitualityLevelRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityRequirementRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityVersionRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityVersionResponse;
import com.shottrack.backend.application.habituality.gateway.AthleteHabitualityGateway;
import com.shottrack.backend.application.habituality.gateway.HabitualityRuleGateway;
import com.shottrack.backend.application.habituality.gateway.HabitualityVersionGateway;
import com.shottrack.backend.application.habituality.model.ActivityType;
import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * UC55: versões, níveis e exigências. Regras de edição (ADR-0023): versão futura é
 * editável e removível; vigente/passada só é editável enquanto nenhum atleta acompanha
 * a regra e nunca é removível. O PUT substitui níveis e exigências por inteiro, na
 * mesma transação (sem cascata no banco: filhos primeiro, em código).
 */
@Service
@RequiredArgsConstructor
public class HabitualityVersionAdminService {

    private final HabitualityRuleGateway ruleGateway;
    private final HabitualityVersionGateway versionGateway;
    private final AthleteHabitualityGateway athleteGateway;
    private final HabitualityVersionResolver resolver;
    private final HabitualityContentReader contentReader;

    public List<HabitualityVersionResponse> list(UUID ruleId) {
        requireRule(ruleId);
        LocalDate today = HabitualityVersionResolver.today();
        UUID currentId = resolver.current(ruleId, today).map(HabitualityRuleVersion::getId).orElse(null);
        boolean tracked = athleteGateway.existsTrackingByRuleId(ruleId);
        return versionGateway.findVersionsByRuleId(ruleId).stream()
                .map(version -> toResponse(version, today, tracked, version.getId().equals(currentId)))
                .toList();
    }

    @Transactional
    public HabitualityVersionResponse create(UUID ruleId, HabitualityVersionRequest request) {
        requireRule(ruleId);
        validateContent(request);

        LocalDate today = HabitualityVersionResolver.today();
        // Primeira versão da regra pode ter data passada ou de hoje; as seguintes, só futura.
        if (versionGateway.ruleHasVersions(ruleId) && !request.validFrom().isAfter(today)) {
            throw new BusinessException("HABITUALITY_VERSION_DATE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (versionGateway.existsByRuleIdAndValidFrom(ruleId, request.validFrom())) {
            throw new BusinessException("HABITUALITY_VERSION_DATE_INVALID", HttpStatus.CONFLICT);
        }

        HabitualityRuleVersion version = versionGateway.saveVersion(HabitualityRuleVersion.builder()
                .ruleId(ruleId)
                .validFrom(request.validFrom())
                .windowMonths(request.windowMonths())
                .build());
        saveContent(version.getId(), request.levels());
        return respond(version, today);
    }

    @Transactional
    public HabitualityVersionResponse update(UUID versionId, HabitualityVersionRequest request) {
        HabitualityRuleVersion version = findVersionOrThrow(versionId);
        LocalDate today = HabitualityVersionResolver.today();
        UUID ruleId = version.getRuleId();

        if (!version.getValidFrom().isAfter(today) && athleteGateway.existsTrackingByRuleId(ruleId)) {
            throw new BusinessException("HABITUALITY_VERSION_NOT_EDITABLE", HttpStatus.CONFLICT);
        }
        validateContent(request);
        validateDateChange(version, request.validFrom(), today);

        version.setValidFrom(request.validFrom());
        version.setWindowMonths(request.windowMonths());
        versionGateway.deleteContent(versionId);
        versionGateway.saveVersion(version);
        saveContent(versionId, request.levels());
        return respond(version, today);
    }

    @Transactional
    public void delete(UUID versionId) {
        HabitualityRuleVersion version = findVersionOrThrow(versionId);
        if (!version.getValidFrom().isAfter(HabitualityVersionResolver.today())) {
            throw new BusinessException("HABITUALITY_VERSION_NOT_EDITABLE", HttpStatus.CONFLICT);
        }
        versionGateway.deleteVersion(version);
    }

    /**
     * Mudar a data: mantém a atual; ou vai para o futuro; ou, se for a versão mais antiga
     * da regra (a "primeira"), pode ficar no passado/hoje. Nunca repete data de outra versão.
     */
    private void validateDateChange(HabitualityRuleVersion version, LocalDate newDate, LocalDate today) {
        if (newDate.equals(version.getValidFrom())) {
            return;
        }
        boolean earliest = versionGateway.findVersionsByRuleId(version.getRuleId()).stream()
                .noneMatch(other -> !other.getId().equals(version.getId())
                        && other.getValidFrom().isBefore(version.getValidFrom()));
        if (!newDate.isAfter(today) && !earliest) {
            throw new BusinessException("HABITUALITY_VERSION_DATE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (versionGateway.existsByRuleIdAndValidFromExcluding(version.getRuleId(), newDate, version.getId())) {
            throw new BusinessException("HABITUALITY_VERSION_DATE_INVALID", HttpStatus.CONFLICT);
        }
    }

    /**
     * Regras que o Bean Validation não expressa: nomes de nível únicos (sem diferenciar
     * maiúsculas) e âmbito mínimo só em exigências que envolvem competição.
     */
    private void validateContent(HabitualityVersionRequest request) {
        Set<String> names = new HashSet<>();
        for (HabitualityLevelRequest level : request.levels()) {
            if (!names.add(level.name().trim().toLowerCase(Locale.ROOT))) {
                throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
            for (HabitualityRequirementRequest requirement : level.requirements()) {
                if (requirement.minScope() != null && requirement.activityType() == ActivityType.TREINO) {
                    throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    private void saveContent(UUID versionId, List<HabitualityLevelRequest> levels) {
        for (HabitualityLevelRequest levelRequest : levels) {
            HabitualityLevel level = versionGateway.saveLevel(HabitualityLevel.builder()
                    .versionId(versionId)
                    .name(levelRequest.name().trim())
                    .position(levelRequest.position())
                    .build());
            for (HabitualityRequirementRequest requirement : levelRequest.requirements()) {
                versionGateway.saveRequirement(HabitualityRequirement.builder()
                        .levelId(level.getId())
                        .activityType(requirement.activityType())
                        .minQuantity(requirement.minQuantity())
                        .onlyInClub(Boolean.TRUE.equals(requirement.onlyInClub()))
                        .distinctEvents(Boolean.TRUE.equals(requirement.distinctEvents()))
                        .minScope(requirement.minScope())
                        .build());
            }
        }
    }

    private HabitualityVersionResponse respond(HabitualityRuleVersion version, LocalDate today) {
        boolean tracked = athleteGateway.existsTrackingByRuleId(version.getRuleId());
        Optional<HabitualityRuleVersion> current = resolver.current(version.getRuleId(), today);
        return toResponse(version, today, tracked, current.map(c -> c.getId().equals(version.getId())).orElse(false));
    }

    private HabitualityVersionResponse toResponse(HabitualityRuleVersion version, LocalDate today, boolean tracked,
                                                  boolean current) {
        boolean future = version.getValidFrom().isAfter(today);
        return new HabitualityVersionResponse(version.getId(), version.getRuleId(), version.getValidFrom(),
                version.getWindowMonths(), current, future || !tracked, future,
                contentReader.levels(version.getId()));
    }

    private void requireRule(UUID ruleId) {
        ruleGateway.findById(ruleId)
                .orElseThrow(() -> new BusinessException("HABITUALITY_RULE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private HabitualityRuleVersion findVersionOrThrow(UUID versionId) {
        return versionGateway.findVersionById(versionId)
                .orElseThrow(() -> new BusinessException("HABITUALITY_VERSION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
