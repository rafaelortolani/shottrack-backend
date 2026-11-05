package com.shottrack.backend.application.plan.gateway.repository;

import com.shottrack.backend.application.plan.model.PlanAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanAuditRepository extends JpaRepository<PlanAudit, UUID> {

    List<PlanAudit> findAllByOrderByAtDescIdDesc(Pageable pageable);
}
