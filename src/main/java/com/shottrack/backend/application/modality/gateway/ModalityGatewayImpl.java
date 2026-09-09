package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.gateway.repository.ModalityRepository;
import com.shottrack.backend.application.modality.model.Modality;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ModalityGatewayImpl implements ModalityGateway {

    private final ModalityRepository modalityRepository;

    @Override
    public List<Modality> findAll() {
        return modalityRepository.findAll();
    }

    @Override
    public Optional<Modality> findById(UUID id) {
        return modalityRepository.findById(id);
    }
}
