package com.shottrack.backend.application.habituality.gateway;

import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agregado versão → níveis → exigências (ADR-0023). Sem cascata no banco: a remoção do
 * conteúdo é explícita, filhos primeiro.
 */
public interface HabitualityVersionGateway {

    HabitualityRuleVersion saveVersion(HabitualityRuleVersion version);

    Optional<HabitualityRuleVersion> findVersionById(UUID id);

    List<HabitualityRuleVersion> findVersionsByRuleId(UUID ruleId);

    boolean existsByRuleIdAndValidFrom(UUID ruleId, LocalDate validFrom);

    boolean existsByRuleIdAndValidFromExcluding(UUID ruleId, LocalDate validFrom, UUID excludedVersionId);

    boolean ruleHasVersions(UUID ruleId);

    HabitualityLevel saveLevel(HabitualityLevel level);

    HabitualityRequirement saveRequirement(HabitualityRequirement requirement);

    List<HabitualityLevel> findLevelsByVersionIds(Collection<UUID> versionIds);

    List<HabitualityRequirement> findRequirementsByLevelIds(Collection<UUID> levelIds);

    /** Apaga exigências e níveis da versão, mantendo a própria versão. */
    void deleteContent(UUID versionId);

    /** Apaga exigências, níveis e a própria versão. */
    void deleteVersion(HabitualityRuleVersion version);
}
