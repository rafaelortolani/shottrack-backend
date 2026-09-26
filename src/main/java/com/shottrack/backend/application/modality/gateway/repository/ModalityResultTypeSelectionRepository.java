package com.shottrack.backend.application.modality.gateway.repository;

import com.shottrack.backend.application.modality.model.ModalityResultTypeSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModalityResultTypeSelectionRepository extends JpaRepository<ModalityResultTypeSelection, UUID> {

    List<ModalityResultTypeSelection> findAllByUserIdAndModalityId(UUID userId, UUID modalityId);

    Optional<ModalityResultTypeSelection> findByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId);

    boolean existsByUserIdAndModalityIdAndResultTypeId(UUID userId, UUID modalityId, UUID resultTypeId);

    /**
     * DELETE em massa executado na hora, em vez do deleteAllBy derivado (que
     * carrega e remove entidade por entidade, só emitindo o DELETE no flush):
     * o Hibernate faz flush dos INSERTs antes dos DELETEs, então remover e
     * readicionar a mesma modalidade na mesma unidade de trabalho violaria a
     * constraint única (user_id, modality_id, result_type_id).
     */
    @Modifying
    @Query("delete from ModalityResultTypeSelection s where s.userId = :userId and s.modalityId = :modalityId")
    void deleteAllByUserIdAndModalityId(@Param("userId") UUID userId, @Param("modalityId") UUID modalityId);
}
