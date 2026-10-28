package com.shottrack.backend.application.habituality.gateway;

import com.shottrack.backend.application.habituality.gateway.repository.HabitualityLevelRepository;
import com.shottrack.backend.application.habituality.gateway.repository.HabitualityRequirementRepository;
import com.shottrack.backend.application.habituality.gateway.repository.HabitualityVersionRepository;
import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class HabitualityVersionGatewayImpl implements HabitualityVersionGateway {

    private final HabitualityVersionRepository versionRepository;
    private final HabitualityLevelRepository levelRepository;
    private final HabitualityRequirementRepository requirementRepository;

    @Override
    public HabitualityRuleVersion saveVersion(HabitualityRuleVersion version) {
        return versionRepository.save(version);
    }

    @Override
    public Optional<HabitualityRuleVersion> findVersionById(UUID id) {
        return versionRepository.findById(id);
    }

    @Override
    public List<HabitualityRuleVersion> findVersionsByRuleId(UUID ruleId) {
        return versionRepository.findAllByRuleIdOrderByValidFromAsc(ruleId);
    }

    @Override
    public boolean existsByRuleIdAndValidFrom(UUID ruleId, LocalDate validFrom) {
        return versionRepository.existsByRuleIdAndValidFrom(ruleId, validFrom);
    }

    @Override
    public boolean existsByRuleIdAndValidFromExcluding(UUID ruleId, LocalDate validFrom, UUID excludedVersionId) {
        return versionRepository.existsByRuleIdAndValidFromAndIdNot(ruleId, validFrom, excludedVersionId);
    }

    @Override
    public boolean ruleHasVersions(UUID ruleId) {
        return versionRepository.existsByRuleId(ruleId);
    }

    @Override
    public HabitualityLevel saveLevel(HabitualityLevel level) {
        return levelRepository.save(level);
    }

    @Override
    public HabitualityRequirement saveRequirement(HabitualityRequirement requirement) {
        return requirementRepository.save(requirement);
    }

    @Override
    public List<HabitualityLevel> findLevelsByVersionIds(Collection<UUID> versionIds) {
        return versionIds.isEmpty() ? List.of() : levelRepository.findAllByVersionIdIn(versionIds);
    }

    @Override
    public List<HabitualityRequirement> findRequirementsByLevelIds(Collection<UUID> levelIds) {
        return levelIds.isEmpty() ? List.of() : requirementRepository.findAllByLevelIdIn(levelIds);
    }

    @Override
    public void deleteContent(UUID versionId) {
        List<UUID> levelIds = levelRepository.findAllByVersionIdIn(List.of(versionId)).stream()
                .map(HabitualityLevel::getId).toList();
        if (!levelIds.isEmpty()) {
            requirementRepository.deleteAllByLevelIdIn(levelIds);
            requirementRepository.flush();
        }
        levelRepository.deleteAllByVersionId(versionId);
        levelRepository.flush();
    }

    @Override
    public void deleteVersion(HabitualityRuleVersion version) {
        deleteContent(version.getId());
        versionRepository.delete(version);
        versionRepository.flush();
    }
}
