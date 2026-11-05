package com.shottrack.backend.application.plan.usecase;

import com.shottrack.backend.application.plan.dto.MyPlanResponse;
import com.shottrack.backend.application.plan.gateway.PlanGateway;
import com.shottrack.backend.application.plan.model.Plan;
import com.shottrack.backend.application.plan.model.PlanLimit;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** UC66: plano, limites efetivos e uso do próprio atleta — usuário, plano e uso (contagem + soma) em consultas fixas, sem N+1. */
@Service
@RequiredArgsConstructor
public class MyPlanService {

    private final UserGateway userGateway;
    private final PlanGateway planGateway;
    private final PlanLimitService planLimitService;
    private final SeriesPhotoGateway photoGateway;

    @Transactional(readOnly = true)
    public MyPlanResponse get(UUID userId) {
        User user = userGateway.findById(userId).orElseThrow(() -> new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED));
        Plan plan = planGateway.findById(user.getPlanId()).orElseThrow();
        Map<PlanLimit, Long> limits = planLimitService.limitsOfPlan(plan.getId());
        SeriesPhotoGateway.Usage usage = photoGateway.usageOf(userId);
        return new MyPlanResponse(
                new MyPlanResponse.PlanRef(plan.getCode(), plan.getName()),
                new MyPlanResponse.Limits(limits.get(PlanLimit.PHOTOS_PER_SERIES), limits.get(PlanLimit.PHOTOS_PER_ATHLETE),
                        limits.get(PlanLimit.STORAGE_BYTES_PER_ATHLETE), limits.get(PlanLimit.UPLOADS_PER_HOUR)),
                new MyPlanResponse.Usage(usage.photos(), usage.storageBytes()));
    }
}
