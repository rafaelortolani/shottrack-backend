package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.gateway.repository.ModalityResultTypeSuggestionRepository;
import com.shottrack.backend.application.modality.model.ModalityResultTypeSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ModalityResultTypeSuggestionGatewayImpl implements ModalityResultTypeSuggestionGateway {

    private final ModalityResultTypeSuggestionRepository modalityResultTypeSuggestionRepository;

    @Override
    public List<ModalityResultTypeSuggestion> findAllByModalityId(UUID modalityId) {
        return modalityResultTypeSuggestionRepository.findAllByModalityId(modalityId);
    }
}
