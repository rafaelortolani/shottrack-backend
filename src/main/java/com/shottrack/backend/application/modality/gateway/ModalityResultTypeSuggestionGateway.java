package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.model.ModalityResultTypeSuggestion;

import java.util.List;
import java.util.UUID;

public interface ModalityResultTypeSuggestionGateway {

    List<ModalityResultTypeSuggestion> findAllByModalityId(UUID modalityId);
}
