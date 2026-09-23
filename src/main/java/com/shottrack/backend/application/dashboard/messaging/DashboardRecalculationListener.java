package com.shottrack.backend.application.dashboard.messaging;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.dashboard.usecase.DashboardRecalculationService;
import com.shottrack.backend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ADR-0015: recalcula do zero as estatísticas do atleta e grava/atualiza
 * dashboard_summary. Recalcular do zero é naturalmente idempotente —
 * reentrega da mesma mensagem produz o mesmo resultado.
 */
@Component
@RequiredArgsConstructor
public class DashboardRecalculationListener {

    private final DashboardRecalculationService dashboardRecalculationService;

    @RabbitListener(queues = RabbitMQConfig.DASHBOARD_RECALCULATION_QUEUE)
    public void onMessage(DashboardRecalculationRequestedEvent event) {
        dashboardRecalculationService.recalculate(event.userId());
    }
}
