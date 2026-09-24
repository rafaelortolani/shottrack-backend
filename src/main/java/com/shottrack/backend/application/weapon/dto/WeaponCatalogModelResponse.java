package com.shottrack.backend.application.weapon.dto;

import java.util.UUID;

/**
 * UC09: modelo do catálogo já com seu tipo (ADR-0004, Revisão 2) — o
 * cliente exibe o tipo, mas não o escolhe.
 */
public record WeaponCatalogModelResponse(
        UUID id,
        String name,
        WeaponTypeResponse type
) {
}
