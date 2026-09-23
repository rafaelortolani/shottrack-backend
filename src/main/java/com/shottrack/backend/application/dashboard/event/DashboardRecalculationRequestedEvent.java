package com.shottrack.backend.application.dashboard.event;

import java.util.UUID;

/**
 * ADR-0015: publicado pelos 9 pontos de escrita (UC33, UC34, UC36, UC38,
 * UC39, UC40, UC41, UC43, UC44) via ApplicationEventPublisher, só depois que a transação
 * principal já commitou. É a mesma classe usada como payload da mensagem
 * AMQP — não existe tradução entre "evento interno" e "mensagem da fila".
 */
public record DashboardRecalculationRequestedEvent(UUID userId) {
}
