package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.PlanTestSupport;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.TestImages;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-0027: requisições SIMULTÂNEAS. Sem transação do teste (cada requisição comita de verdade, em threads próprias), com
 * planos e contas criados aqui e removidos no fim. Os planos de teste usam o prefixo CNC_.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlanConcurrencyTest {

    private static final String PASSWORD = "senha12345";
    private static final String PREFIX = "CNC_";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private final List<String> emails = new ArrayList<>();

    @AfterEach
    void cleanUp() throws Exception {
        // contas pela exclusão oficial (UC52): leva fotos, séries e arquivos
        for (String email : emails) {
            String token = login(email);
            if (token != null) {
                mockMvc.perform(post("/api/users/me/deletion").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + PASSWORD + "\"}"));
            }
        }
        // volta ao estado original, em passos (o índice único do padrão é conferido linha a linha, não ao fim do comando)
        jdbc.update("UPDATE plans SET is_default = FALSE");
        jdbc.update("UPDATE plans SET is_default = TRUE, active = TRUE WHERE code = 'FREE'");
        jdbc.update("DELETE FROM plan_limits WHERE plan_id IN (SELECT id FROM plans WHERE code LIKE ?)", PREFIX + "%");
        jdbc.update("DELETE FROM plans WHERE code LIKE ?", PREFIX + "%");
        // a auditoria do banco de teste começa vazia (os outros testes contam linhas); estas requisições comitaram de verdade
        jdbc.update("DELETE FROM plan_audit");
    }

    private String register(String label) throws Exception {
        String email = "conc." + UUID.randomUUID() + "@shottrack.com";
        emails.add(email);
        return TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, label, email, PASSWORD);
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        if (result.getResponse().getStatus() != 200) {
            return null;
        }
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("accessToken").asText();
    }

    private String admin() throws Exception {
        String email = "conc.admin." + UUID.randomUUID() + "@shottrack.com";
        emails.add(email);
        String token = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository, "Admin Conc", email, PASSWORD);
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        return token;
    }

    private void createPlan(String admin, String code) throws Exception {
        mockMvc.perform(post("/api/admin/plans").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"name\":\"Plano " + code + "\"}")).andExpect(status().isCreated());
    }

    private <T> List<T> runTogether(int threads, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await();
        go.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(120, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }

    private UUID seriesOf(String token) throws Exception {
        UUID training = SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC");
        var result = mockMvc.perform(post("/api/series").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainingId\":\"" + training + "\"}")).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    private int upload(String token, UUID series, byte[] image) throws Exception {
        return mockMvc.perform(multipart("/api/series/" + series + "/photos")
                .file(new org.springframework.mock.web.MockMultipartFile("file", "a.jpg", "image/jpeg", image))
                .header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus();
    }

    private String athleteOnPlan(String admin, String planCode, Map<String, Object> limits) throws Exception {
        createPlan(admin, planCode);
        mockMvc.perform(patch("/api/admin/plans/" + planCode + "/limits").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("limits", limits)))).andExpect(status().isOk());
        String token = register("Atleta Conc");
        UUID athleteId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, emails.get(emails.size() - 1));
        mockMvc.perform(put("/api/admin/users/" + athleteId + "/plan").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"planCode\":\"" + planCode + "\"}")).andExpect(status().isOk());
        return token;
    }

    @Test
    void shouldNeverExceedThePerSeriesLimitWithSimultaneousUploads() throws Exception {
        String admin = admin();
        String token = athleteOnPlan(admin, PREFIX + "SERIES", Map.of("PHOTOS_PER_SERIES", 3));
        UUID series = seriesOf(token);
        byte[] image = TestImages.jpeg(40, 40);

        List<Integer> statuses = runTogether(8, () -> upload(token, series, image));

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(3);
        assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_photos WHERE series_id = ?", Long.class, series)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT position FROM series_photos WHERE series_id = ? ORDER BY position", Integer.class, series))
                .as("posições sem repetição").containsExactly(1, 2, 3);
    }

    @Test
    void shouldNeverExceedThePerAthleteLimitWithSimultaneousUploadsInDifferentSeries() throws Exception {
        String admin = admin();
        String token = athleteOnPlan(admin, PREFIX + "ATHLETE", Map.of("PHOTOS_PER_ATHLETE", 2));
        List<UUID> series = List.of(seriesOf(token), seriesOf(token), seriesOf(token), seriesOf(token), seriesOf(token), seriesOf(token));
        byte[] image = TestImages.jpeg(40, 40);
        java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();

        List<Integer> statuses = runTogether(6, () -> upload(token, series.get(next.getAndIncrement()), image));

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_photos sp JOIN users u ON u.id = sp.athlete_id WHERE u.email = ?", Long.class,
                emails.get(emails.size() - 1))).isEqualTo(2);
    }

    @Test
    void shouldNeverExceedTheStorageLimitWithSimultaneousUploads() throws Exception {
        String admin = admin();
        byte[] image = TestImages.jpeg(40, 40);
        String probeToken = register("Atleta Medida");
        UUID probeSeries = seriesOf(probeToken);
        upload(probeToken, probeSeries, image);
        long size = jdbc.queryForObject("SELECT size_bytes FROM series_photos WHERE series_id = ?", Long.class, probeSeries);

        String token = athleteOnPlan(admin, PREFIX + "STORAGE", Map.of("STORAGE_BYTES_PER_ATHLETE", size * 2));
        UUID series = seriesOf(token);

        List<Integer> statuses = runTogether(6, () -> upload(token, series, image));

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT sum(size_bytes) FROM series_photos WHERE series_id = ?", Long.class, series)).isLessThanOrEqualTo(size * 2);
    }

    @Test
    void shouldAlwaysKeepExactlyOneActiveDefaultPlanWithSimultaneousChanges() throws Exception {
        String admin = admin();
        for (String code : List.of(PREFIX + "A", PREFIX + "B", PREFIX + "C", PREFIX + "D")) {
            createPlan(admin, code);
        }
        List<String> codes = List.of(PREFIX + "A", PREFIX + "B", PREFIX + "C", PREFIX + "D", "FREE", PREFIX + "A", PREFIX + "C", PREFIX + "B");
        java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();

        List<Integer> statuses = runTogether(8, () -> mockMvc.perform(patch("/api/admin/plans/" + codes.get(next.getAndIncrement()))
                .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"default\":true}")).andReturn().getResponse().getStatus());

        assertThat(statuses).allMatch(s -> s == 200);
        assertThat(jdbc.queryForList("SELECT code FROM plans WHERE is_default AND active", String.class)).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plans WHERE is_default", Long.class)).isEqualTo(1);
    }

    @Test
    void shouldNeverExceedTwentyPlansWithSimultaneousCreations() throws Exception {
        String admin = admin();
        long existing = jdbc.queryForObject("SELECT count(*) FROM plans", Long.class);
        for (long i = existing; i < 18; i++) {
            createPlan(admin, PREFIX + "P" + i);
        }
        java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();

        List<Integer> statuses = runTogether(6, () -> {
            String code = PREFIX + "Q" + next.getAndIncrement();
            return mockMvc.perform(post("/api/admin/plans").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\",\"name\":\"Plano " + code + "\"}")).andReturn().getResponse().getStatus();
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM plans", Long.class)).isLessThanOrEqualTo(20);
        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(20 - Math.max(existing, 18));
        assertThat(statuses.stream().filter(s -> s != 201)).allMatch(s -> s == 409);
    }
}
