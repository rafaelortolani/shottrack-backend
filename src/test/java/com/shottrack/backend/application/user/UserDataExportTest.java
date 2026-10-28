package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.application.user.gateway.repository.UserRepository;
import com.shottrack.backend.support.AthleteDataTestSupport;
import com.shottrack.backend.support.AthleteDataTestSupport.Athlete;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserDataExportTest {

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
    void shouldExportEverythingTheSystemKeepsAboutTheAthlete() throws Exception {
        Athlete athlete = populatedAthlete("atleta.exporta@shottrack.com");

        export(athlete)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.profile.name").value("Atleta Teste"))
                .andExpect(jsonPath("$.profile.email").value(athlete.email()))
                .andExpect(jsonPath("$.profile.experienceLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.profile.registeredAt").isNotEmpty())
                .andExpect(jsonPath("$.profile.termsAcceptedAt").isNotEmpty())
                .andExpect(jsonPath("$.profile.termsVersion").value("2026-10"))
                .andExpect(jsonPath("$.practicedModalities.length()").value(1))
                .andExpect(jsonPath("$.practicedModalities[0].modality").value("IPSC"))
                .andExpect(jsonPath("$.practicedModalities[0].resultTypes", org.hamcrest.Matchers.hasItems("Tempo", "Pontuação")))
                .andExpect(jsonPath("$.weapons.length()").value(1))
                .andExpect(jsonPath("$.weapons[0].brand").value("Glock"))
                .andExpect(jsonPath("$.weapons[0].model").value("G17"))
                .andExpect(jsonPath("$.weapons[0].caliber").value("9mm"))
                .andExpect(jsonPath("$.weapons[0].type").isNotEmpty())
                .andExpect(jsonPath("$.weapons[0].nickname").value("Minha G17"))
                .andExpect(jsonPath("$.ammunitions.length()").value(1))
                .andExpect(jsonPath("$.ammunitions[0].nickname").value("Munição de teste"))
                .andExpect(jsonPath("$.accessories.length()").value(1))
                .andExpect(jsonPath("$.accessories[0].name").value("Luneta 4x32"))
                .andExpect(jsonPath("$.accessories[0].type").value("Luneta"))
                .andExpect(jsonPath("$.accessories[0].notes").value("comprada usada"))
                .andExpect(jsonPath("$.accessories[0].weapons.length()").value(1))
                .andExpect(jsonPath("$.accessories[0].weapons[0].model").value("G17"))
                .andExpect(jsonPath("$.trainingLocations.length()").value(1))
                .andExpect(jsonPath("$.trainingLocations[0].name").value("Clube de Tiro Central"))
                .andExpect(jsonPath("$.trainingLocations[0].city").value("São Paulo"))
                .andExpect(jsonPath("$.trainingLocations[0].state").value("SP"))
                .andExpect(jsonPath("$.trainingLocations[0].club").value(false))
                .andExpect(jsonPath("$.visits.length()").value(1))
                .andExpect(jsonPath("$.visits[0].trainingLocation").value("Clube de Tiro Central"))
                .andExpect(jsonPath("$.visits[0].startedAt").isNotEmpty())
                .andExpect(jsonPath("$.visits[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.visits[0].visitType").value("TREINO"))
                .andExpect(jsonPath("$.visits[0].competitionScope").isEmpty())
                .andExpect(jsonPath("$.visits[0].trainings.length()").value(1))
                .andExpect(jsonPath("$.visits[0].trainings[0].modality").value("IPSC"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series.length()").value(1))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].weapon.nickname").value("Minha G17"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].ammunition.nickname").value("Munição de teste"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].distanceMeters").value(25))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].target").value("Alvo IPSC"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].shotCount").value(10))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].notes").value("primeira série"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].results.length()").value(3))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].results[?(@.resultType=='Tempo')].value").value("9.8"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].results[?(@.resultType=='Pontuação')].value").value("0"))
                .andExpect(jsonPath("$.visits[0].trainings[0].series[0].results[?(@.resultType=='Fator de desempenho')].notApplicable").value(true))
                .andExpect(jsonPath("$.habituality.tracking.rule").value(org.hamcrest.Matchers.startsWith("Regra de teste")))
                .andExpect(jsonPath("$.habituality.tracking.level").value("Iniciante"))
                .andExpect(jsonPath("$.habituality.personalGoalYearlyTrainings").value(40));
    }

    @Test
    void shouldNotLeakPasswordHashTokensOrInternalIds() throws Exception {
        Athlete athlete = populatedAthlete("atleta.semvazamento@shottrack.com");
        String passwordHash = userRepository.findByEmail(athlete.email()).orElseThrow().getPasswordHash();

        String body = export(athlete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain(passwordHash)
                .doesNotContain("$2a$").doesNotContain("$2b$")
                .doesNotContain("passwordHash").doesNotContain("password")
                .doesNotContain(athlete.refreshToken()).doesNotContain(athlete.accessToken())
                .doesNotContain("refresh").doesNotContain("tokenHash").doesNotContain("verification")
                .doesNotContain("\"id\"").doesNotContain("Id\"")
                .doesNotContain(athlete.userId().toString());
    }

    @Test
    void shouldExportOnlyTheAuthenticatedAthletesData() throws Exception {
        Athlete first = populatedAthlete("atleta.primeiro@shottrack.com");
        Athlete second = populatedAthlete("atleta.segundo@shottrack.com");

        String firstBody = export(first).andExpect(status().isOk())
                .andExpect(jsonPath("$.weapons.length()").value(1))
                .andExpect(jsonPath("$.visits.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String secondBody = export(second).andExpect(status().isOk())
                .andExpect(jsonPath("$.weapons.length()").value(1))
                .andExpect(jsonPath("$.visits.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(firstBody).contains(first.email()).doesNotContain(second.email());
        assertThat(secondBody).contains(second.email()).doesNotContain(first.email());
    }

    @Test
    void shouldExportEmptyListsForAthleteWithoutData() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.exportavazio@shottrack.com");

        export(athlete)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value(athlete.email()))
                .andExpect(jsonPath("$.practicedModalities").isEmpty())
                .andExpect(jsonPath("$.weapons").isEmpty())
                .andExpect(jsonPath("$.ammunitions").isEmpty())
                .andExpect(jsonPath("$.accessories").isEmpty())
                .andExpect(jsonPath("$.trainingLocations").isEmpty())
                .andExpect(jsonPath("$.visits").isEmpty())
                .andExpect(jsonPath("$.habituality.tracking").isEmpty())
                .andExpect(jsonPath("$.habituality.personalGoalYearlyTrainings").isEmpty());
    }

    @Test
    void shouldReturnJsonAttachmentWithDatedFilenameAndNoEnvelope() throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, "atleta.cabecalho@shottrack.com");

        var result = export(athlete)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/json")))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .matches("attachment; filename=\"shottrack-dados-\\d{8}\\.json\"");
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me/export"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private Athlete populatedAthlete(String email) throws Exception {
        Athlete athlete = AthleteDataTestSupport.registerAthlete(mockMvc, objectMapper, pendingRegistrationRepository,
                userRepository, email);
        AthleteDataTestSupport.populate(mockMvc, objectMapper, pendingRegistrationRepository, athlete);
        AthleteDataTestSupport.populateHabituality(mockMvc, objectMapper, jdbcTemplate, athlete);
        return athlete;
    }

    private ResultActions export(Athlete athlete) throws Exception {
        return mockMvc.perform(get("/api/users/me/export")
                .header("Authorization", "Bearer " + athlete.accessToken()));
    }
}
