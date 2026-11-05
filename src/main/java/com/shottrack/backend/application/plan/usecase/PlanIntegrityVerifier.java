package com.shottrack.backend.application.plan.usecase;

import com.shottrack.backend.application.plan.gateway.PlanGateway;
import com.shottrack.backend.application.plan.model.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * ADR-0027: sempre existe exatamente UM plano padrão e ele está ativo. O banco só garante "no máximo um" (índice parcial)
 * e "padrão é ativo" (check); o "pelo menos um" é conferido aqui, na subida — sem padrão, o cadastro (UC23) não teria
 * a quem associar o atleta novo, então a aplicação falha de forma clara em vez de subir quebrada.
 */
@Component
@RequiredArgsConstructor
public class PlanIntegrityVerifier implements ApplicationRunner {

    private final PlanGateway planGateway;

    @Override
    public void run(ApplicationArguments args) {
        verify();
    }

    /** Falha com IllegalStateException se não houver exatamente um plano padrão ativo. */
    public void verify() {
        defaultPlan();
    }

    /** O plano padrão (recebe os cadastros novos, UC23). */
    public UUID defaultPlanId() {
        return defaultPlan().getId();
    }

    private Plan defaultPlan() {
        List<Plan> defaults = planGateway.findDefaults();
        if (defaults.size() != 1) {
            throw new IllegalStateException("Integridade dos planos: esperado exatamente 1 plano padrão, encontrado " + defaults.size());
        }
        if (!defaults.get(0).isActive()) {
            throw new IllegalStateException("Integridade dos planos: o plano padrão precisa estar ativo");
        }
        return defaults.get(0);
    }
}
