package com.shottrack.backend.application.habituality.usecase;

import com.shottrack.backend.application.habituality.gateway.HabitualityVersionGateway;
import com.shottrack.backend.application.habituality.model.HabitualityRuleVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * ADR-0023: versão em vigor = a de maior validFrom que não passa de hoje; a próxima
 * (aviso de mudança) = a de menor validFrom depois de hoje. "Hoje" é sempre em UTC,
 * como nas demais janelas do projeto (ADR-0022).
 */
@Component
@RequiredArgsConstructor
class HabitualityVersionResolver {

    private final HabitualityVersionGateway versionGateway;

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    Optional<HabitualityRuleVersion> current(UUID ruleId, LocalDate today) {
        return versionGateway.findVersionsByRuleId(ruleId).stream()
                .filter(version -> !version.getValidFrom().isAfter(today))
                .max(Comparator.comparing(HabitualityRuleVersion::getValidFrom));
    }

    Optional<HabitualityRuleVersion> upcoming(UUID ruleId, LocalDate today) {
        return versionGateway.findVersionsByRuleId(ruleId).stream()
                .filter(version -> version.getValidFrom().isAfter(today))
                .min(Comparator.comparing(HabitualityRuleVersion::getValidFrom));
    }
}
