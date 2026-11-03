package com.shottrack.backend.application.user.dto;

import com.shottrack.backend.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRegistrationRequest(
        @NotBlank(message = "token é obrigatório")
        @Size(max = 255, message = "token deve ter no máximo 255 caracteres")
        String token,

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
        String name,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
        @MaxUtf8Bytes(72)
        String password,

        /** ADR-0019: sem aceite explícito (true) o cadastro não conclui — validado no usecase (TERMS_NOT_ACCEPTED). */
        Boolean acceptedTerms
) {
}
