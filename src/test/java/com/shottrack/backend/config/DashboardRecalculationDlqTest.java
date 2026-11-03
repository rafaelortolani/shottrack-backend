package com.shottrack.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** S17: a DLQ expira mensagens (TTL, 7 dias por padrão) e o recoverer não leva texto de exceção (pode ter e-mail) pra ela. */
@SpringBootTest
class DashboardRecalculationDlqTest {

    @Autowired
    @Qualifier("dashboardRecalculationDeadLetterQueue")
    private Queue dlq;

    @Test
    void shouldDeclareTheDlqWithAMessageTtl() {
        assertThat(dlq.getArguments().get("x-message-ttl")).isEqualTo(TimeUnit.DAYS.toMillis(7));
    }

    @Test
    void shouldNotCarryTheExceptionMessageOrCauseTextToTheDlq() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        var recoverer = new RabbitMQConfig().dashboardRecalculationRecoverer(template);
        Throwable failure = new ListenerExecutionFailedException("falha", new IllegalStateException(
                "Key (email)=(x@y.com) already exists", new RuntimeException("causa com a.b@c.org")),
                new Message(new byte[0], new MessageProperties()));

        recoverer.recover(new Message("{}".getBytes(), new MessageProperties()), failure);

        var captor = forClass(Message.class);
        verify(template).send(org.mockito.ArgumentMatchers.eq(RabbitMQConfig.DASHBOARD_RECALCULATION_DLX),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.DASHBOARD_RECALCULATION_DLQ), captor.capture());
        var headers = captor.getValue().getMessageProperties().getHeaders();
        String carried = String.valueOf(headers.get("x-exception-message")) + headers.get("x-exception-stacktrace");

        assertThat(carried).doesNotContain("x@y.com").doesNotContain("a.b@c.org").doesNotContain("already exists");
        assertThat(carried).contains("IllegalStateException");
    }
}
