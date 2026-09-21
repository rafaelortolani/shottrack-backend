package com.shottrack.backend.application.modality.gateway.repository;

import com.shottrack.backend.application.modality.model.ModalityResultTypeSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModalityResultTypeSelectionRepository extends JpaRepository<ModalityResultTypeSelection, UUID> {

    List<ModalityResultTypeSelection> findAllByUserIdAndModalityId(UUID userId, UUID modalityId);

    Optional<ModalityResultTypeSelection> findByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId);

    boolean existsByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId);
}
