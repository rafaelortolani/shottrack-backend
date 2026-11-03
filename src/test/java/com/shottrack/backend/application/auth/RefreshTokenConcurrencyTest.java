package com.shottrack.backend.application.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.support.AthleteDataTestSupport;
import com.shottrack.backend.support.AthleteDataTestSupport.Athlete;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S20: a rotação do refresh token é atômica. Duas chamadas simultâneas com o MESMO token: só uma vence; a outra
 * é reuso (ADR-0001) e derruba as sessões do atleta. NÃO é @Transactional de propósito — a corrida só existe com
 * commits reais; o teste apaga o atleta pela rota real no fim (só dados do Flyway podem sobrar no banco).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenConcurrencyTest {

    private static final int ROUNDS = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldLetOnlyOneOfTwoSimultaneousRefreshCallsWinAndDropTheSessionsOnReuse() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.concorrencia@shottrack.com");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            String current = athlete.refreshToken();
            for (int round = 0; round < ROUNDS; round++) {
                List<Integer> statuses = Collections.synchronizedList(new ArrayList<>());
                List<String> newTokens = Collections.synchronizedList(new ArrayList<>());
                CyclicBarrier barrier = new CyclicBarrier(2);
                String sameToken = current;

                List<Future<?>> calls = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    calls.add(pool.submit(() -> {
                        barrier.await();
                        var response = refresh(sameToken).andReturn().getResponse();
                        statuses.add(response.getStatus());
                        if (response.getStatus() == 200) {
                            JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
                            newTokens.add(data.get("refreshToken").asText());
                        }
                        return null;
                    }));
                }
                for (Future<?> call : calls) {
                    call.get();
                }

                assertThat(statuses).as("rodada %d: só uma chamada vence", round).containsExactlyInAnyOrder(200, 401);
                assertThat(newTokens).hasSize(1);

                // o token original agora é reuso: 401 e todas as sessões caem — inclusive a do vencedor
                refresh(current).andExpect(status().isUnauthorized());
                refresh(newTokens.get(0)).andExpect(status().isUnauthorized());

                current = login(athlete);
            }
        } finally {
            pool.shutdownNow();
            mockMvc.perform(post("/api/users/me/deletion")
                            .header("Authorization", "Bearer " + athlete.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteAccountRequest(AthleteDataTestSupport.PASSWORD))))
                    .andExpect(status().isOk());
        }

        assertThat(AthleteDataTestSupport.countRows(jdbcTemplate, athlete)).allSatisfy((table, rows) ->
                assertThat(rows).as("sobrou lixo de teste na tabela %s", table).isZero());
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(token))));
    }

    private String login(Athlete athlete) throws Exception {
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.shottrack.backend.application.auth.dto.LoginRequest(athlete.email(), AthleteDataTestSupport.PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse();
        return objectMapper.readTree(response.getContentAsString()).get("data").get("refreshToken").asText();
    }
}
