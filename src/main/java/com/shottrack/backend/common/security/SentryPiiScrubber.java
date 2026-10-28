package com.shottrack.backend.common.security;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * ADR-0019: última barreira antes de um evento sair pro Sentry. Com
 * send-default-pii=false o SDK já não anexa usuário/IP/cabeçalhos sensíveis,
 * mas mensagens de exceção (ex: violação de unique do Postgres, que ecoa o
 * valor) e do log podem trazer e-mail ou token — aqui eles são mascarados, e
 * requisição (corpo, cabeçalhos, cookies, query) e usuário são descartados.
 */
@Component
public class SentryPiiScrubber implements SentryOptions.BeforeSendCallback {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+");

    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        Message message = event.getMessage();
        if (message != null) {
            message.setMessage(scrub(message.getMessage()));
            message.setFormatted(scrub(message.getFormatted()));
        }

        if (event.getExceptions() != null) {
            for (SentryException exception : event.getExceptions()) {
                exception.setValue(scrub(exception.getValue()));
            }
        }

        Request request = event.getRequest();
        if (request != null) {
            request.setHeaders(null);
            request.setCookies(null);
            request.setData(null);
            request.setQueryString(null);
        }
        event.setUser(null);

        return event;
    }

    static String scrub(String text) {
        if (text == null) {
            return null;
        }
        String scrubbed = EMAIL.matcher(text).replaceAll("***@***");
        scrubbed = JWT.matcher(scrubbed).replaceAll("***");
        return BEARER.matcher(scrubbed).replaceAll("Bearer ***");
    }
}
