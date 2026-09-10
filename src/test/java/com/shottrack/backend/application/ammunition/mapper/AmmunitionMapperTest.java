package com.shottrack.backend.application.ammunition.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o branch de nulidade gerado pelo MapStruct (AmmunitionMapperImpl) pra
 * quando os três parâmetros são nulos ao mesmo tempo — nunca acontece via
 * AmmunitionService, que sempre passa a munição salva (nunca nula); só
 * fabricante/calibre podem ser nulos individualmente, e isso já é coberto
 * pelos testes de cadastro/edição (cadastro só com apelido, por exemplo).
 */
class AmmunitionMapperTest {

    private final AmmunitionMapper mapper = new AmmunitionMapperImpl();

    @Test
    void shouldReturnNullWhenEverythingIsNull() {
        assertThat(mapper.toResponse(null, null, null)).isNull();
    }
}
