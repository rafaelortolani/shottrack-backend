package com.shottrack.backend.application.user.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o branch de nulidade gerado pelo MapStruct (UserMapperImpl) — nunca
 * acontece via serviço, que sempre resolve o usuário
 * (findById().orElseThrow()) antes de mapear.
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void shouldReturnNullWhenUserIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
