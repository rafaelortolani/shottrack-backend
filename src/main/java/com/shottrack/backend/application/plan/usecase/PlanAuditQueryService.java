package com.shottrack.backend.application.plan.usecase;

import com.shottrack.backend.application.plan.dto.PlanAuditResponse;
import com.shottrack.backend.application.plan.dto.PlanAuditResponse.UserRef;
import com.shottrack.backend.application.plan.gateway.PlanAuditGateway;
import com.shottrack.backend.application.plan.model.PlanAudit;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** UC67: histórico mais recente primeiro; referência a conta excluída (nula, UC52) vira "excluída", sem nome nem e-mail. */
@Service
@RequiredArgsConstructor
public class PlanAuditQueryService {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;

    private final PlanAuditGateway auditGateway;
    private final UserGateway userGateway;

    @Transactional(readOnly = true)
    public List<PlanAuditResponse> recent(Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        List<PlanAudit> entries = auditGateway.findRecent(size);

        Set<UUID> userIds = new HashSet<>();
        entries.forEach(entry -> {
            if (entry.getActorUserId() != null) {
                userIds.add(entry.getActorUserId());
            }
            if (entry.getTargetUserId() != null) {
                userIds.add(entry.getTargetUserId());
            }
        });
        Map<UUID, User> users = userGateway.findAllByIdIn(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return entries.stream().map(entry -> new PlanAuditResponse(
                entry.getAt(), entry.getAction(),
                ref(entry.getActorUserId(), users),
                entry.getTargetType(), entry.getTargetPlanCode(),
                "USER".equals(entry.getTargetType()) ? ref(entry.getTargetUserId(), users) : null,
                entry.getFieldKey(), entry.getOldValue(), entry.getNewValue())).toList();
    }

    private static UserRef ref(UUID userId, Map<UUID, User> users) {
        User user = userId == null ? null : users.get(userId);
        return user == null ? new UserRef(null, null, true) : new UserRef(user.getName(), user.getEmail(), false);
    }
}
