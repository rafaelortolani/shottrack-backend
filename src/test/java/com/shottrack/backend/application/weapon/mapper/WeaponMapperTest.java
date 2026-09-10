package com.shottrack.backend.application.weapon.mapper;

import com.shottrack.backend.application.weapon.model.WeaponBrand;
import com.shottrack.backend.application.weapon.model.WeaponCaliber;
import com.shottrack.backend.application.weapon.model.WeaponModel;
import com.shottrack.backend.application.weapon.model.WeaponType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre os branches de nulidade gerados pelo MapStruct (WeaponMapperImpl),
 * nunca exercitados via HTTP porque WeaponService sempre resolve o catálogo
 * (findById().orElseThrow()) antes de mapear — essas entradas nulas só
 * acontecem se o mapper for chamado direto, como aqui.
 */
class WeaponMapperTest {

    private final WeaponMapper mapper = new WeaponMapperImpl();

    @Test
    void shouldReturnNullWhenEverythingIsNull() {
        assertThat(mapper.toResponse(null, null, null, null, null)).isNull();
    }

    @Test
    void shouldReturnNullTypeResponseWhenTypeIsNull() {
        assertThat(mapper.toResponse((WeaponType) null)).isNull();
    }

    @Test
    void shouldReturnNullBrandResponseWhenBrandIsNull() {
        assertThat(mapper.toResponse((WeaponBrand) null)).isNull();
    }

    @Test
    void shouldReturnNullModelResponseWhenModelIsNull() {
        assertThat(mapper.toResponse((WeaponModel) null)).isNull();
    }

    @Test
    void shouldReturnNullCaliberResponseWhenCaliberIsNull() {
        assertThat(mapper.toResponse((WeaponCaliber) null)).isNull();
    }
}
