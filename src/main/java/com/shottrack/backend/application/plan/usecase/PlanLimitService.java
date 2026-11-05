package com.shottrack.backend.application.plan.usecase;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shottrack.backend.application.plan.gateway.PlanLimitGateway;
import com.shottrack.backend.application.plan.model.PlanLimit;
import com.shottrack.backend.application.plan.model.PlanLimitValue;
import com.shottrack.backend.application.user.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-0027: todo limite vem do plano do atleta (nunca de propriedade). Sem valor gravado, vale o padrão do enum.
 * Cache em memória de até 60 s (atleta → plano e plano → limites), invalidado quando o administrador altera limite ou plano.
 */
@Service
@RequiredArgsConstructor
public class PlanLimitService {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final UserGateway userGateway;
    private final PlanLimitGateway planLimitGateway;

    private final Cache<UUID, UUID> planByAthlete = Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(100_000).build();
    private final Cache<UUID, Map<PlanLimit, Long>> limitsByPlan = Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(1_000).build();

    public long limitFor(UUID athleteId, PlanLimit key) {
        return limitsOf(athleteId).get(key);
    }

    /** Todos os limites efetivos do atleta. Atleta sem plano legível (conta excluída em voo) usa os padrões do enum. */
    public Map<PlanLimit, Long> limitsOf(UUID athleteId) {
        UUID planId = planByAthlete.getIfPresent(athleteId);
        if (planId == null) {
            planId = userGateway.findPlanIdById(athleteId).orElse(null);
            if (planId == null) {
                return defaults();
            }
            planByAthlete.put(athleteId, planId);
        }
        return limitsOfPlan(planId);
    }

    /** Limites efetivos de um plano (valor gravado ou, na falta, o padrão do enum). */
    public Map<PlanLimit, Long> limitsOfPlan(UUID planId) {
        return limitsByPlan.get(planId, id -> {
            Map<PlanLimit, Long> limits = defaults();
            for (PlanLimitValue stored : planLimitGateway.findAllByPlanId(id)) {
                PlanLimit.fromKey(stored.getId().limitKey()).ifPresent(limit -> limits.put(limit, stored.getValue()));
            }
            return limits;
        });
    }

    /**
     * Esquece o que está em cache. Chamada logo na alteração (quem lê na mesma transação já vê o novo valor) e de novo
     * depois do fim da transação, com commit ou rollback (uma leitura no meio não deixa valor desatualizado guardado).
     */
    public void invalidate() {
        invalidateNow();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    invalidateNow();
                }
            });
        }
    }

    private void invalidateNow() {
        planByAthlete.invalidateAll();
        limitsByPlan.invalidateAll();
    }

    private static Map<PlanLimit, Long> defaults() {
        Map<PlanLimit, Long> limits = new EnumMap<>(PlanLimit.class);
        for (PlanLimit limit : PlanLimit.values()) {
            limits.put(limit, limit.getDefaultValue());
        }
        return limits;
    }
}
