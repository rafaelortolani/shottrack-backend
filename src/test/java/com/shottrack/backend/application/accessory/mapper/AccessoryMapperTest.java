package com.shottrack.backend.application.accessory.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o branch de nulidade gerado pelo MapStruct (AccessoryMapperImpl) pra
 * quando acessório e lista de armas são nulos ao mesmo tempo — nunca
 * acontece via AccessoryService, que sempre passa o acessório salvo (nunca
 * nulo) e, no mínimo, uma lista vazia de armas associadas.
 */
class AccessoryMapperTest {

    private final AccessoryMapper mapper = new AccessoryMapperImpl();

    @Test
    void shouldReturnNullWhenEverythingIsNull() {
        assertThat(mapper.toResponse(null, null)).isNull();
    }
}
