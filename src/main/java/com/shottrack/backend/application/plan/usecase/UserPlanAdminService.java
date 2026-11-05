package com.shottrack.backend.application.plan.usecase;

import com.shottrack.backend.application.plan.dto.AdminUserResponse;
import com.shottrack.backend.application.plan.gateway.PlanGateway;
import com.shottrack.backend.application.plan.model.Plan;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC65 (ADR-0027): o administrador localiza o atleta e troca o plano dele. A busca é só leitura e devolve o mínimo
 * (nome, e-mail, plano): sem hash de senha, tokens ou papel, e nunca lista todos os usuários.
 */
@Service
@RequiredArgsConstructor
public class UserPlanAdminService {

    static final int MIN_SEARCH_CHARS = 3;
    static final int MAX_SEARCH_RESULTS = 10;

    private final UserGateway userGateway;
    private final PlanGateway planGateway;
    private final PlanLimitService planLimitService;
    private final PlanAuditRecorder auditRecorder;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchByEmail(String email) {
        String fragment = email == null ? "" : email.strip();
        if (fragment.length() < MIN_SEARCH_CHARS) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        List<User> users = userGateway.searchByEmailFragment(escapeLike(fragment), MAX_SEARCH_RESULTS);
        Map<UUID, Plan> plans = plansOf(users);
        return users.stream().map(user -> toResponse(user, plans.get(user.getPlanId()))).toList();
    }

    @Transactional
    public AdminUserResponse assign(UUID actorId, UUID userId, String planCode) {
        planGateway.lockPlanAdministration();
        User user = userGateway.findById(userId).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND));
        Plan plan = planGateway.findByCode(planCode.strip()).orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!plan.isActive()) {
            throw new BusinessException("PLAN_NOT_ACTIVE", HttpStatus.CONFLICT);
        }
        if (!plan.getId().equals(user.getPlanId())) {
            String oldCode = planGateway.findById(user.getPlanId()).map(Plan::getCode).orElse(null);
            user.setPlanId(plan.getId());
            userGateway.save(user);
            auditRecorder.planAssigned(actorId, user.getId(), oldCode, plan.getCode());
            planLimitService.invalidate();
        }
        return toResponse(user, plan);
    }

    private Map<UUID, Plan> plansOf(List<User> users) {
        return users.stream().map(User::getPlanId).distinct()
                .map(planGateway::findById).flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Plan::getId, Function.identity()));
    }

    private static AdminUserResponse toResponse(User user, Plan plan) {
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), plan.getCode(), plan.getName());
    }

    /** `!`, `%` e `_` são literais na busca (o `escape '!'` está na consulta). */
    private static String escapeLike(String fragment) {
        return fragment.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
