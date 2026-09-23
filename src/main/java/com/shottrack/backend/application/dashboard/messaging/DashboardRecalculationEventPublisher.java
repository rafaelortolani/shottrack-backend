package com.shottrack.backend.application.dashboard.messaging;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ADR-0015: só publica na fila DEPOIS que a transação principal já
 * commitou (TransactionPhase.AFTER_COMMIT) — nunca antes, pra não disparar
 * recálculo de uma escrita que pode dar rollback. O evento interno
 * (ApplicationEventPublisher, publicado pelos 6 pontos de escrita) é
 * síncrono e sempre disparado; é só a entrega efetiva na fila que espera
 * o commit.
 */
@Component
@RequiredArgsConstructor
public class DashboardRecalculationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDashboardRecalculationRequested(DashboardRecalculationRequestedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.DASHBOARD_RECALCULATION_QUEUE, event);
    }
}
