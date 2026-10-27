package com.shottrack.backend.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0019 + ADR-0018: limite por usuário autenticado em UC52 (exclusão) e UC53 (exportação).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.account-deletion.attempts=2",
        "app.rate-limit.data-export.attempts=2"
})
class AccountRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldLimitDataExportPerUser() throws Exception {
        Athlete athlete = athlete("limite.exportacao@shottrack.com");

        export(athlete).andExpect(status().isOk());
        export(athlete).andExpect(status().isOk());
        export(athlete)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldKeepExportLimitIndependentBetweenUsers() throws Exception {
        Athlete first = athlete("limite.usuario.a@shottrack.com");
        Athlete second = athlete("limite.usuario.b@shottrack.com");

        export(first).andExpect(status().isOk());
        export(first).andExpect(status().isOk());
        export(first).andExpect(status().isTooManyRequests());

        export(second).andExpect(status().isOk());
    }

    @Test
    void shouldLimitAccountDeletionAttemptsPerUserEvenWithWrongPassword() throws Exception {
        Athlete athlete = athlete("limite.exclusao@shottrack.com");

        deletion(athlete, "senhaErrada1").andExpect(status().isUnauthorized());
        deletion(athlete, "senhaErrada2").andExpect(status().isUnauthorized());
        deletion(athlete, "senhaErrada3")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldNotCountUnauthenticatedRequestsAgainstAnyone() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/users/me/export")).andExpect(status().isUnauthorized());
        }
    }

    private Athlete athlete(String email) throws Exception {
        return AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository, userRepository, email);
    }

    private ResultActions export(Athlete athlete) throws Exception {
        return mockMvc.perform(get("/api/users/me/export").header("Authorization", "Bearer " + athlete.accessToken()));
    }

    private ResultActions deletion(Athlete athlete, String password) throws Exception {
        return mockMvc.perform(post("/api/users/me/deletion")
                .header("Authorization", "Bearer " + athlete.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeleteAccountRequest(password))));
    }
}
