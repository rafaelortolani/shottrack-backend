package com.shottrack.backend.application.habituality.usecase;

import com.shottrack.backend.application.habituality.dto.HabitualityLevelResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityRequirementResponse;
import com.shottrack.backend.application.habituality.gateway.HabitualityVersionGateway;
import com.shottrack.backend.application.habituality.mapper.HabitualityMapper;
import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Carrega níveis e exigências de uma versão (duas consultas, sem N+1) já ordenados por
 * posição, no formato das respostas.
 */
@Component
@RequiredArgsConstructor
class HabitualityContentReader {

    private final HabitualityVersionGateway versionGateway;
    private final HabitualityMapper mapper;

    record LevelContent(HabitualityLevel level, List<HabitualityRequirement> requirements) {
    }

    List<LevelContent> content(UUID versionId) {
        List<HabitualityLevel> levels = versionGateway.findLevelsByVersionIds(List.of(versionId)).stream()
                .sorted(Comparator.comparingInt(HabitualityLevel::getPosition).thenComparing(HabitualityLevel::getName))
                .toList();
        Map<UUID, List<HabitualityRequirement>> requirementsByLevelId = versionGateway
                .findRequirementsByLevelIds(levels.stream().map(HabitualityLevel::getId).toList()).stream()
                .collect(Collectors.groupingBy(HabitualityRequirement::getLevelId));
        return levels.stream()
                .map(level -> new LevelContent(level, requirementsByLevelId.getOrDefault(level.getId(), List.of())))
                .toList();
    }

    List<HabitualityLevelResponse> levels(UUID versionId) {
        return content(versionId).stream().map(this::toResponse).toList();
    }

    HabitualityLevelResponse toResponse(LevelContent content) {
        List<HabitualityRequirementResponse> requirements = content.requirements().stream()
                .map(mapper::toRequirementResponse).toList();
        return mapper.toLevelResponse(content.level(), requirements);
    }
}
