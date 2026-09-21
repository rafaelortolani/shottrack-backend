package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.gateway.repository.ModalityResultTypeSelectionRepository;
import com.shottrack.backend.application.modality.model.ModalityResultTypeSelection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ModalityResultTypeSelectionGatewayImpl implements ModalityResultTypeSelectionGateway {

    private final ModalityResultTypeSelectionRepository modalityResultTypeSelectionRepository;

    @Override
    public ModalityResultTypeSelection save(ModalityResultTypeSelection selection) {
        return modalityResultTypeSelectionRepository.save(selection);
    }

    @Override
    public List<ModalityResultTypeSelection> findAllByUserIdAndModalityId(UUID userId, UUID modalityId) {
        return modalityResultTypeSelectionRepository.findAllByUserIdAndModalityId(userId, modalityId);
    }

    @Override
    public Optional<ModalityResultTypeSelection> findByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId) {
        return modalityResultTypeSelectionRepository.findByUserIdAndModalityIdAndResultTypeId(userId, modalityId, resultTypeId);
    }

    @Override
    public boolean existsByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId) {
        return modalityResultTypeSelectionRepository.existsByUserIdAndModalityIdAndResultTypeId(userId, modalityId, resultTypeId);
    }

    @Override
    public void delete(ModalityResultTypeSelection selection) {
        modalityResultTypeSelectionRepository.delete(selection);
    }
}
