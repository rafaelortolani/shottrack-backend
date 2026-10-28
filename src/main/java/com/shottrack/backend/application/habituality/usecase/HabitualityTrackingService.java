package com.shottrack.backend.application.habituality.usecase;

import com.shottrack.backend.application.habituality.dto.AthleteHabitualityRuleResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityTrackingRequest;
import com.shottrack.backend.application.habituality.dto.HabitualityTrackingResponse;
import com.shottrack.backend.application.habituality.dto.UpcomingVersionResponse;
import com.shottrack.backend.application.habituality.gateway.AthleteHabitualityGateway;
import com.shottrack.backend.application.habituality.gateway.HabitualityRuleGateway;
import com.shottrack.backend.application.habituality.model.AthleteHabitualityTracking;
import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import com.shottrack.backend.application.habituality.model.HabitualityRule;
import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC56: o atleta escolhe a regra e o nível que acompanha (um por atleta). O nível é
 * guardado pelo nome — o id muda a cada versão, o nome sobrevive (ADR-0023).
 */
@Service
@RequiredArgsConstructor
public class HabitualityTrackingService {

    private final HabitualityRuleGateway ruleGateway;
    private final AthleteHabitualityGateway athleteGateway;
    private final HabitualityVersionResolver resolver;
    private final HabitualityContentReader contentReader;

    /** Regras ativas que têm versão em vigor (sem versão vigente, a regra não aparece). */
    public List<AthleteHabitualityRuleResponse> listRules() {
        LocalDate today = HabitualityVersionResolver.today();
        return ruleGateway.findAllActive().stream()
                .map(rule -> resolver.current(rule.getId(), today)
                        .map(version -> toResponse(rule, version, today))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public HabitualityTrackingResponse track(UUID userId, HabitualityTrackingRequest request) {
        HabitualityRule rule = ruleGateway.findById(request.ruleId())
                .orElseThrow(() -> new BusinessException("HABITUALITY_RULE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!rule.isActive()) {
            throw new BusinessException("HABITUALITY_RULE_INACTIVE", HttpStatus.CONFLICT);
        }
        HabitualityRuleVersion version = resolver.current(rule.getId(), HabitualityVersionResolver.today())
                .orElseThrow(() -> new BusinessException("HABITUALITY_VERSION_NOT_FOUND", HttpStatus.NOT_FOUND));
        HabitualityLevel level = contentReader.content(version.getId()).stream()
                .map(HabitualityContentReader.LevelContent::level)
                .filter(candidate -> candidate.getName().equalsIgnoreCase(request.levelName().trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("HABITUALITY_LEVEL_NOT_FOUND", HttpStatus.NOT_FOUND));

        AthleteHabitualityTracking tracking = athleteGateway.findTrackingByUserId(userId)
                .map(existing -> {
                    existing.setRuleId(rule.getId());
                    existing.setLevelName(level.getName());
                    return existing;
                })
                .orElseGet(() -> AthleteHabitualityTracking.builder()
                        .userId(userId)
                        .ruleId(rule.getId())
                        .levelName(level.getName())
                        .build());
        AthleteHabitualityTracking saved = athleteGateway.saveTracking(tracking);
        return new HabitualityTrackingResponse(saved.getRuleId(), saved.getLevelName());
    }

    /** Sem acompanhamento não é erro: o resultado desejado (não acompanhar) já vale. */
    public void stopTracking(UUID userId) {
        athleteGateway.deleteTrackingByUserId(userId);
    }

    private AthleteHabitualityRuleResponse toResponse(HabitualityRule rule, HabitualityRuleVersion version, LocalDate today) {
        UpcomingVersionResponse upcoming = resolver.upcoming(rule.getId(), today)
                .map(next -> new UpcomingVersionResponse(next.getValidFrom()))
                .orElse(null);
        return new AthleteHabitualityRuleResponse(rule.getId(), rule.getName(), rule.getLegalReference(),
                version.getValidFrom(), version.getWindowMonths(), contentReader.levels(version.getId()), upcoming);
    }
}
