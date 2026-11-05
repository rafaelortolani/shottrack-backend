package com.shottrack.backend.application.plan.gateway;

import com.shottrack.backend.application.plan.gateway.repository.PlanRepository;
import com.shottrack.backend.application.plan.model.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PlanGatewayImpl implements PlanGateway {

    /** Chave do lock consultivo da administração de planos (constante qualquer, só precisa ser a mesma). */
    private static final long ADMIN_LOCK_KEY = 0x5348_4F54_504C_414EL;

    private final PlanRepository planRepository;

    @Override
    public Optional<Plan> findById(UUID id) {
        return planRepository.findById(id);
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return planRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<Plan> findAll() {
        return planRepository.findAllByOrderByNameAsc();
    }

    @Override
    public List<Plan> findDefaults() {
        return planRepository.findAllByDefaultPlanTrue();
    }

    @Override
    public long count() {
        return planRepository.count();
    }

    @Override
    public boolean existsByCode(String code) {
        return planRepository.existsByCodeIgnoreCase(code);
    }

    @Override
    public boolean existsByName(String name) {
        return planRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameExcluding(String name, UUID planId) {
        return planRepository.existsByNameIgnoreCaseAndIdNot(name, planId);
    }

    @Override
    public Plan save(Plan plan) {
        return planRepository.save(plan);
    }

    @Override
    public Plan saveAndFlush(Plan plan) {
        return planRepository.saveAndFlush(plan);
    }

    @Override
    public void lockPlanAdministration() {
        planRepository.advisoryLock(ADMIN_LOCK_KEY);
    }
}
