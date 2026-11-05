package com.shottrack.backend.application.plan.mapper;

import com.shottrack.backend.application.plan.dto.PlanLimitResponse;
import com.shottrack.backend.application.plan.dto.PlanResponse;
import com.shottrack.backend.application.plan.model.Plan;
import com.shottrack.backend.application.plan.model.PlanLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanMapper {

    @Mapping(target = "key", expression = "java(limit.name())")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "customized", expression = "java(value != limit.getDefaultValue())")
    PlanLimitResponse toLimitResponse(PlanLimit limit, long value);

    @Mapping(target = "code", source = "plan.code")
    @Mapping(target = "name", source = "plan.name")
    @Mapping(target = "active", source = "plan.active")
    @Mapping(target = "defaultPlan", source = "plan.defaultPlan")
    @Mapping(target = "athleteCount", source = "athleteCount")
    @Mapping(target = "limits", source = "limits")
    PlanResponse toResponse(Plan plan, long athleteCount, List<PlanLimitResponse> limits);
}
