package com.shottrack.backend.application.ammunition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edição parcial (UC15/ADR-0007): campo ausente/null significa "não alterar" —
 * diferente da edição de arma, que substitui tudo.
 */
public record AmmunitionUpdateRequest(
        UUID manufacturerId,
        UUID caliberId,
        @Size(max = 100, message = "apelido deve ter no máximo 100 caracteres")
        String nickname,
        @Digits(integer = 4, fraction = 2, message = "peso do projétil deve ter até 4 dígitos inteiros e 2 decimais")
        @DecimalMin(value = "0", message = "peso do projétil não pode ser negativo")
        BigDecimal projectileWeightGrains,
        @Digits(integer = 4, fraction = 2, message = "carga de pólvora deve ter até 4 dígitos inteiros e 2 decimais")
        @DecimalMin(value = "0", message = "carga de pólvora não pode ser negativa")
        BigDecimal powderCharge,
        @Size(max = 50, message = "tipo de projétil deve ter no máximo 50 caracteres")
        String projectileType,
        @Size(max = 50, message = "lote deve ter no máximo 50 caracteres")
        String lot,
        @Size(max = 2000, message = "observações deve ter no máximo 2000 caracteres")
        String notes
) {
}
