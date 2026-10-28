package com.shottrack.backend.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0019: erros e logs não ecoam dados pessoais recebidos na requisição.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class ErrorPrivacyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Test
    void shouldNotEchoMalformedBodyInResponseOrLogs(CapturedOutput output) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"segredo.pessoal@shottrack.com\", \"password\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("segredo.pessoal");
        assertThat(output.getAll()).doesNotContain("segredo.pessoal");
    }

    @Test
    void shouldNotEchoInvalidUuidInBodyOrQueryParameterErrors(CapturedOutput output) throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Teste", "atleta.privacidade@shottrack.com", "senha12345");

        var body = mockMvc.perform(post("/api/practiced-modalities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modalityId\": \"valor-sigiloso-1\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        var query = mockMvc.perform(get("/api/series")
                        .header("Authorization", "Bearer " + token)
                        .param("weaponId", "valor-sigiloso-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andReturn().getResponse().getContentAsString();
        var pathVariable = mockMvc.perform(get("/api/weapons/valor-sigiloso-3")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("valor-sigiloso");
        assertThat(query).doesNotContain("valor-sigiloso");
        assertThat(pathVariable).doesNotContain("valor-sigiloso");
        assertThat(output.getAll()).doesNotContain("valor-sigiloso");
    }

    @Test
    void shouldMaskEmailsAndTokensInTheJsonLogFile() throws Exception {
        String marker = UUID.randomUUID().toString();
        LoggerFactory.getLogger(ErrorPrivacyTest.class)
                .error("marcador {} violação: Key (email)=(vazou.no.log@shottrack.com) token eyJhbGciOiJIUzI1.eyJzdWIiOiIx.c2lnbmF0dXJl", marker);

        String logLine = Files.readAllLines(Path.of(System.getProperty("LOG_PATH", "logs"), "shottrack-backend.json")).stream()
                .filter(line -> line.contains(marker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("linha do marcador não encontrada no arquivo de log JSON"));

        assertThat(logLine).contains("***@***").doesNotContain("vazou.no.log@shottrack.com")
                .doesNotContain("eyJhbGciOiJIUzI1");
    }
}
