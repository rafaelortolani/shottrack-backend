package com.shottrack.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ADR-0015: fila de recálculo do dashboard. Retry com backoff é feito no
 * consumidor (client-side, via RetryInterceptorBuilder) — depois de
 * esgotar as tentativas, RepublishMessageRecoverer republica a mensagem
 * na dead-letter queue em vez de descartar ou reenfileirar pra sempre.
 */
@Configuration
public class RabbitMQConfig {

    public static final String DASHBOARD_RECALCULATION_QUEUE = "dashboard.recalculation";
    public static final String DASHBOARD_RECALCULATION_DLQ = "dashboard.recalculation.dlq";
    private static final String DASHBOARD_RECALCULATION_DLX = "dashboard.recalculation.dlx";

    @Bean
    public Queue dashboardRecalculationQueue() {
        return QueueBuilder.durable(DASHBOARD_RECALCULATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DASHBOARD_RECALCULATION_DLX)
                .withArgument("x-dead-letter-routing-key", DASHBOARD_RECALCULATION_DLQ)
                .build();
    }

    @Bean
    public DirectExchange dashboardRecalculationDeadLetterExchange() {
        return new DirectExchange(DASHBOARD_RECALCULATION_DLX);
    }

    @Bean
    public Queue dashboardRecalculationDeadLetterQueue() {
        return QueueBuilder.durable(DASHBOARD_RECALCULATION_DLQ).build();
    }

    @Bean
    public Binding dashboardRecalculationDeadLetterBinding() {
        return BindingBuilder.bind(dashboardRecalculationDeadLetterQueue())
                .to(dashboardRecalculationDeadLetterExchange())
                .with(DASHBOARD_RECALCULATION_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                                 Jackson2JsonMessageConverter rabbitMessageConverter,
                                                                                 RabbitTemplate rabbitTemplate) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(5)
                .backOffOptions(1000, 2.0, 30000)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, DASHBOARD_RECALCULATION_DLX, DASHBOARD_RECALCULATION_DLQ))
                .build());
        return factory;
    }
}
