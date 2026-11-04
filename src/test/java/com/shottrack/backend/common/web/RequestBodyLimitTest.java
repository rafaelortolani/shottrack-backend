package com.shottrack.backend.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.common.exception.ApiExceptionHandler;
import com.shottrack.backend.support.TestUsers;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** S10: teto de 64 KB (app.request.max-body-bytes) para o corpo das requisições → 413 PAYLOAD_TOO_LARGE. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RequestBodyLimitTest {

    private static final int LIMIT = 65536;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    /** JSON de login com exatamente {@code totalBytes} bytes (preenchido no campo password). */
    private static String loginBodyOfSize(int totalBytes) {
        String prefix = "{\"email\":\"limite.corpo@shottrack.com\",\"password\":\"";
        String suffix = "\"}";
        return prefix + "p".repeat(totalBytes - prefix.length() - suffix.length()) + suffix;
    }

    private ResultActions loginWith(String body, String language) throws Exception {
        var builder = post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body);
        if (language != null) {
            builder.header("Accept-Language", language);
        }
        return mockMvc.perform(builder);
    }

    @Test
    void shouldAcceptABodyExactlyAtTheLimit() throws Exception {
        String body = loginBodyOfSize(LIMIT);
        assertThat(body.getBytes(StandardCharsets.UTF_8)).hasSize(LIMIT);

        loginWith(body, null).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectABodyOneByteAboveTheLimitBeforeAnyProcessing() throws Exception {
        String body = loginBodyOfSize(LIMIT + 1);

        loginWith(body, null)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("PAYLOAD_TOO_LARGE"))
                .andExpect(jsonPath("$.error.message").value("O corpo da requisição é grande demais"));
    }

    @Test
    void shouldTranslateTheMessageToEnglish() throws Exception {
        loginWith(loginBodyOfSize(LIMIT + 1), "en")
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.message").value("The request body is too large"));
    }

    @Test
    void shouldRejectHugeBodiesOnAuthenticatedRoutesToo() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta", "atleta.corpogrande@shottrack.com", "senha12345");
        String huge = "{\"trainingId\":\"" + java.util.UUID.randomUUID() + "\",\"notes\":\"" + "n".repeat(200_000) + "\"}";

        mockMvc.perform(post("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(huge))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("PAYLOAD_TOO_LARGE"));
    }

    @Test
    void shouldKeepNormalSmallRequestsWorking() throws Exception {
        loginWith("{\"email\":\"limite.corpo@shottrack.com\",\"password\":\"senha12345\"}", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    // ---- corpo sem Content-Length (chunked): o stream interrompe ao passar do teto ----

    private static final class ChunkedRequest extends HttpServletRequestWrapper {
        private final byte[] content;

        ChunkedRequest(HttpServletRequest request, byte[] content) {
            super(request);
            this.content = content;
        }

        @Override
        public long getContentLengthLong() {
            return -1;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(content);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                }
            };
        }
    }

    private static void drain(jakarta.servlet.ServletRequest request) throws IOException {
        try (var in = request.getInputStream()) {
            in.readAllBytes();
        }
    }

    @Test
    void shouldInterruptAChunkedBodyThatGrowsPastTheLimit() throws Exception {
        RequestBodyLimitFilter filter = new RequestBodyLimitFilter(10, 100, mock(HandlerExceptionResolver.class));
        MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/auth/login");
        FilterChain chain = (request, response) -> drain(request);

        assertThatThrownBy(() -> filter.doFilter(new ChunkedRequest(raw, new byte[11]), new MockHttpServletResponse(), chain))
                .isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void shouldLetAChunkedBodyAtTheLimitThrough() throws Exception {
        RequestBodyLimitFilter filter = new RequestBodyLimitFilter(10, 100, mock(HandlerExceptionResolver.class));
        MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/auth/login");
        int[] read = {0};
        FilterChain chain = (request, response) -> read[0] = request.getInputStream().readAllBytes().length;

        filter.doFilter(new ChunkedRequest(raw, new byte[10]), new MockHttpServletResponse(), chain);

        assertThat(read[0]).isEqualTo(10);
    }

    @Test
    void shouldAllowTheLargerBodyOnlyOnThePhotoUploadRoute() throws Exception {
        RequestBodyLimitFilter filter = new RequestBodyLimitFilter(10, 100, mock(HandlerExceptionResolver.class));
        FilterChain chain = (request, response) -> drain(request);

        // ADR-0026: 50 bytes passam do teto comum (10) mas cabem no teto do upload de foto (100)
        MockHttpServletRequest photo = new MockHttpServletRequest("POST", "/api/series/" + java.util.UUID.randomUUID() + "/photos");
        photo.setContent(new byte[50]);
        MockHttpServletResponse photoResponse = new MockHttpServletResponse();
        filter.doFilter(photo, photoResponse, chain);
        assertThat(photoResponse.getStatus()).isEqualTo(200);

        MockHttpServletRequest other = new MockHttpServletRequest("POST", "/api/series");
        other.setContent(new byte[50]);
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        new RequestBodyLimitFilter(10, 100, resolver).doFilter(other, new MockHttpServletResponse(), chain);
        org.mockito.Mockito.verify(resolver).resolveException(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(com.shottrack.backend.common.exception.BusinessException.class));

        // e mesmo no upload há um teto: 101 bytes de Content-Length são recusados
        HandlerExceptionResolver photoResolver = mock(HandlerExceptionResolver.class);
        MockHttpServletRequest tooBig = new MockHttpServletRequest("POST", "/api/series/" + java.util.UUID.randomUUID() + "/photos");
        tooBig.setContent(new byte[101]);
        new RequestBodyLimitFilter(10, 100, photoResolver).doFilter(tooBig, new MockHttpServletResponse(), chain);
        org.mockito.Mockito.verify(photoResolver).resolveException(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(com.shottrack.backend.common.exception.BusinessException.class));
    }

    @Test
    void shouldAnswer413WhenTheBodyReadIsInterruptedByTheLimit() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        ApiExceptionHandler handler = new ApiExceptionHandler(messages);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt"));
        try {
            ResponseEntity<?> response = handler.handleUnreadableInput(new HttpMessageNotReadableException(
                    "I/O error", new RequestBodyTooLargeException(10), mock(HttpInputMessage.class)));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody().toString()).contains("PAYLOAD_TOO_LARGE");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
