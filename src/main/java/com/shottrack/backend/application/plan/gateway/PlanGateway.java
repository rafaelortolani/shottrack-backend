package com.shottrack.backend.application.plan.gateway;

import com.shottrack.backend.application.plan.model.Plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanGateway {

    Optional<Plan> findById(UUID id);

    Optional<Plan> findByCode(String code);

    List<Plan> findAll();

    /** Planos marcados como padrão (a integridade exige exatamente um). */
    List<Plan> findDefaults();

    long count();

    boolean existsByCode(String code);

    boolean existsByName(String name);

    boolean existsByNameExcluding(String name, UUID planId);

    Plan save(Plan plan);

    Plan saveAndFlush(Plan plan);

    /**
     * Trava (até o fim da transação) toda alteração administrativa de planos: garante que duas requisições
     * simultâneas não criem plano acima do teto nem deixem dois padrões ou nenhum.
     */
    void lockPlanAdministration();
}
