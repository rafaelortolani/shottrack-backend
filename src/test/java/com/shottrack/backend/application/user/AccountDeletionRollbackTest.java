package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.application.weapon.gateway.WeaponGateway;
import com.shottrack.backend.support.AthleteDataTestSupport;
import com.shottrack.backend.support.AthleteDataTestSupport.Athlete;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC52, fluxo 3a: rollback completo. Este teste NÃO é @Transactional de
 * propósito — dentro da transação do teste, "rollback" nunca seria observável.
 * Os dados são commitados de verdade, então o próprio teste termina apagando a
 * conta pela rota real (só dados do Flyway podem sobrar no banco).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountDeletionRollbackTest {

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

    @SpyBean
    private WeaponGateway weaponGateway;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldRollBackEverythingWhenAStepFailsMidWay() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.rollback@shottrack.com");
        try {
            AthleteDataTestSupport.populate(mockMvc, objectMapper, pendingRegistrationRepository, athlete);
            // o resumo do dashboard é gravado pelo consumidor da fila (assíncrono)
            await().atMost(Duration.ofSeconds(15)).until(() ->
                    AthleteDataTestSupport.countRows(jdbcTemplate, athlete).get("dashboard_summary") > 0);
            Map<String, Long> before = withoutDashboard(AthleteDataTestSupport.countRows(jdbcTemplate, athlete));
            clearInvocations(mailSender);

            // armas são apagadas depois de séries, visitas, acessórios e munições — esses passos já
            // rodaram quando a falha acontece e precisam ser desfeitos
            doThrow(new IllegalStateException("falha simulada no meio da exclusão"))
                    .when(weaponGateway).deleteAllByUserId(any());

            deleteAccount(athlete)
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));

            assertThat(withoutDashboard(AthleteDataTestSupport.countRows(jdbcTemplate, athlete))).isEqualTo(before);
            assertThat(AthleteDataTestSupport.countRows(jdbcTemplate, athlete).get("dashboard_summary")).isPositive();
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        } finally {
            Mockito.reset(weaponGateway);
            deleteAccount(athlete).andExpect(status().isOk());
        }

        assertThat(AthleteDataTestSupport.countRows(jdbcTemplate, athlete)).allSatisfy((table, rows) ->
                assertThat(rows).as("sobrou lixo de teste na tabela %s", table).isZero());
    }

    private org.springframework.test.web.servlet.ResultActions deleteAccount(Athlete athlete) throws Exception {
        return mockMvc.perform(post("/api/users/me/deletion")
                .header("Authorization", "Bearer " + athlete.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeleteAccountRequest(AthleteDataTestSupport.PASSWORD))));
    }

    /** As tabelas do resumo são regravadas pela fila a cada evento; o resto é o que a exclusão mexe. */
    private static Map<String, Long> withoutDashboard(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("dashboard_summary"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
