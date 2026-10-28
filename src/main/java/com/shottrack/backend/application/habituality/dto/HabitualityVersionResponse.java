package com.shottrack.backend.application.habituality.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC55: current marca a versão em vigor; editable/removable resumem as regras de edição
 * (futura: editável e removível; vigente/passada: editável só sem atleta acompanhando,
 * nunca removível).
 */
public record HabitualityVersionResponse(
        UUID id,
        UUID ruleId,
        LocalDate validFrom,
        int windowMonths,
        boolean current,
        boolean editable,
        boolean removable,
        List<HabitualityLevelResponse> levels
) {
}
