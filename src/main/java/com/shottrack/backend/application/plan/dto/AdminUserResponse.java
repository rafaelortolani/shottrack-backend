package com.shottrack.backend.application.plan.dto;

import java.util.UUID;

/** UC65: o mínimo para o administrador identificar o atleta e ver o plano — sem hash de senha, tokens, papel nem outros dados. */
public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        String planCode,
        String planName
) {
}
