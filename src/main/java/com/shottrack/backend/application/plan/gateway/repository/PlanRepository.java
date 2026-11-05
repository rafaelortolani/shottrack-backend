package com.shottrack.backend.application.plan.gateway.repository;

import com.shottrack.backend.application.plan.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByCodeIgnoreCase(String code);

    List<Plan> findAllByDefaultPlanTrue();

    List<Plan> findAllByOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Serializa as alterações administrativas de planos até o fim da transação (ver PlanGateway). */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:key)) l", nativeQuery = true)
    int advisoryLock(@Param("key") long key);
}
