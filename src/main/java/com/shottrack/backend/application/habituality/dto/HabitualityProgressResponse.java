package com.shottrack.backend.application.habituality.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * UC57: sem acompanhamento, rule é null. ruleInactive e levelRequired dispensam a
 * apuração (compliant e requirements ficam nulos).
 */
public record HabitualityProgressResponse(
        ProgressRuleResponse rule,
        String level,
        Integer windowMonths,
        LocalDate periodStart,
        LocalDate periodEnd,
        Boolean compliant,
        List<ProgressRequirementResponse> requirements,
        UpcomingVersionResponse upcomingChange,
        boolean levelRequired,
        boolean ruleInactive,
        PersonalGoalResponse personalGoal
) {
}
