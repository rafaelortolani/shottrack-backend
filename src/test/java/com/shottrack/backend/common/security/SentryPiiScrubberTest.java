package com.shottrack.backend.common.security;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SentryPiiScrubberTest {

    private final SentryPiiScrubber scrubber = new SentryPiiScrubber();

    @Test
    void shouldMaskEmailsAndTokensInMessageAndExceptionValues() {
        SentryEvent event = new SentryEvent();
        Message message = new Message();
        message.setMessage("falha para atleta@shottrack.com com token eyJhbGciOi.eyJzdWIiOi.assinatura-xyz");
        message.setFormatted("falha para atleta@shottrack.com");
        event.setMessage(message);

        SentryException exception = new SentryException();
        exception.setValue("duplicate key: Key (email)=(outro.atleta@shottrack.com) already exists; Authorization: Bearer abc.def-123");
        event.setExceptions(List.of(exception));

        SentryEvent scrubbed = scrubber.execute(event, new Hint());

        assertThat(scrubbed.getMessage().getMessage()).doesNotContain("atleta@shottrack.com").doesNotContain("eyJhbGciOi")
                .contains("***@***");
        assertThat(scrubbed.getMessage().getFormatted()).doesNotContain("atleta@shottrack.com");
        assertThat(scrubbed.getExceptions().get(0).getValue())
                .doesNotContain("outro.atleta@shottrack.com").doesNotContain("abc.def-123")
                .contains("Bearer ***");
    }

    @Test
    void shouldDropRequestDataHeadersCookiesAndUser() {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setHeaders(Map.of("Authorization", "Bearer segredo", "Cookie", "sessao=segredo"));
        request.setCookies("sessao=segredo");
        request.setData("{\"password\":\"segredo\"}");
        request.setQueryString("email=atleta@shottrack.com");
        event.setRequest(request);
        User user = new User();
        user.setEmail("atleta@shottrack.com");
        user.setIpAddress("203.0.113.9");
        event.setUser(user);

        SentryEvent scrubbed = scrubber.execute(event, new Hint());

        assertThat(scrubbed.getRequest().getHeaders()).isNull();
        assertThat(scrubbed.getRequest().getCookies()).isNull();
        assertThat(scrubbed.getRequest().getData()).isNull();
        assertThat(scrubbed.getRequest().getQueryString()).isNull();
        assertThat(scrubbed.getUser()).isNull();
    }

    @Test
    void shouldAcceptEventWithoutMessageExceptionsOrRequest() {
        assertThat(scrubber.execute(new SentryEvent(), new Hint())).isNotNull();
    }
}
