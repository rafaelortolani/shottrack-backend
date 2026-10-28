package com.shottrack.backend.application.habituality.mapper;

import com.shottrack.backend.application.habituality.dto.HabitualityLevelResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityRequirementResponse;
import com.shottrack.backend.application.habituality.dto.HabitualityRuleResponse;
import com.shottrack.backend.application.habituality.model.HabitualityLevel;
import com.shottrack.backend.application.habituality.model.HabitualityRequirement;
import com.shottrack.backend.application.habituality.model.HabitualityRule;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitualityMapper {

    HabitualityRuleResponse toRuleResponse(HabitualityRule rule);

    HabitualityRequirementResponse toRequirementResponse(HabitualityRequirement requirement);

    HabitualityLevelResponse toLevelResponse(HabitualityLevel level, List<HabitualityRequirementResponse> requirements);
}
