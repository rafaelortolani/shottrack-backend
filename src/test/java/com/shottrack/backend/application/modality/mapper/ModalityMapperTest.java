package com.shottrack.backend.application.modality.mapper;

import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.modality.model.ResultType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o branch de nulidade gerado pelo MapStruct (ModalityMapperImpl) —
 * nunca acontece via serviço, que sempre resolve a modalidade/tipo de
 * resultado (findById().orElseThrow()) antes de mapear.
 */
class ModalityMapperTest {

    private final ModalityMapper mapper = new ModalityMapperImpl();

    @Test
    void shouldReturnNullWhenModalityIsNull() {
        assertThat(mapper.toResponse((Modality) null)).isNull();
    }

    @Test
    void shouldReturnNullWhenResultTypeIsNull() {
        assertThat(mapper.toResponse((ResultType) null)).isNull();
    }
}
