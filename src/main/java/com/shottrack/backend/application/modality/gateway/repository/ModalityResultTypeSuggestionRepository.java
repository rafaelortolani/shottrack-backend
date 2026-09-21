package com.shottrack.backend.application.modality.gateway.repository;

import com.shottrack.backend.application.modality.model.ModalityResultTypeSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModalityResultTypeSuggestionRepository extends JpaRepository<ModalityResultTypeSuggestion, UUID> {

    List<ModalityResultTypeSuggestion> findAllByModalityId(UUID modalityId);
}
