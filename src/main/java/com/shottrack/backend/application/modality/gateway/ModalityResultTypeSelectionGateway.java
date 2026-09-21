package com.shottrack.backend.application.modality.gateway;

import com.shottrack.backend.application.modality.model.ModalityResultTypeSelection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModalityResultTypeSelectionGateway {

    ModalityResultTypeSelection save(ModalityResultTypeSelection selection);

    List<ModalityResultTypeSelection> findAllByUserIdAndModalityId(UUID userId, UUID modalityId);

    Optional<ModalityResultTypeSelection> findByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId);

    boolean existsByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId);

    void delete(ModalityResultTypeSelection selection);
}
