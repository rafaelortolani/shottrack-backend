package com.shottrack.backend.application.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.shottrack.backend.support.PlanApiTest;
import com.shottrack.backend.support.SeriesTestSupport;
import com.shottrack.backend.support.SqlRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC65: a busca do administrador traz o uso (fotos e espaço) de cada atleta, numa única consulta agregada. */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.shottrack.backend.support.SqlRecorder")
class UserSearchUsageTest extends PlanApiTest {

    private void photos(String token, int count) throws Exception {
        UUID series = PlanEnforcementTest.createSeries(mockMvc, objectMapper, token, SeriesTestSupport.openTraining(mockMvc, objectMapper, token, "IPSC"));
        for (int i = 0; i < count; i++) {
            PlanEnforcementTest.upload(mockMvc, token, series).andExpect(status().isCreated());
        }
    }

    private JsonNode search(String admin, String fragment) throws Exception {
        return data(call(HttpMethod.GET, "/api/admin/users?email=" + fragment, admin, null).andExpect(status().isOk()));
    }

    private long storageOf(String email) {
        return jdbc.queryForObject("SELECT coalesce(sum(size_bytes), 0) FROM series_photos sp JOIN users u ON u.id = sp.athlete_id WHERE u.email = ?",
                Long.class, email);
    }

    @Test
    void shouldReportPhotoCountAndStorageBytesAndZerosWithoutPhotos() throws Exception {
        String admin = admin();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String withPhotos = "uso.com." + tag + "@shottrack.com";
        String without = "uso.sem." + tag + "@shottrack.com";
        photos(athlete(withPhotos), 2);
        athlete(without);

        JsonNode found = search(admin, "uso.com." + tag);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).get("photoCount").asLong()).isEqualTo(2);
        assertThat(found.get(0).get("storageBytes").asLong()).isPositive().isEqualTo(storageOf(withPhotos));

        JsonNode empty = search(admin, "uso.sem." + tag);
        assertThat(empty.get(0).get("photoCount").asLong()).isZero();
        assertThat(empty.get(0).get("storageBytes").asLong()).isZero();
    }

    @Test
    void shouldNotCountPhotosOfAnotherAthlete() throws Exception {
        String admin = admin();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String one = "outro.a." + tag + "@shottrack.com";
        String other = "outro.b." + tag + "@shottrack.com";
        photos(athlete(one), 1);
        photos(athlete(other), 3);

        assertThat(search(admin, "outro.a." + tag).get(0).get("photoCount").asLong()).isEqualTo(1);
        assertThat(search(admin, "outro.a." + tag).get(0).get("storageBytes").asLong()).isEqualTo(storageOf(one));
        assertThat(search(admin, "outro.b." + tag).get(0).get("photoCount").asLong()).isEqualTo(3);
    }

    @Test
    void shouldNotExposePhotoIdsNorKeys() throws Exception {
        String admin = admin();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        photos(athlete("ids." + tag + "@shottrack.com"), 1);

        String body = call(HttpMethod.GET, "/api/admin/users?email=ids." + tag, admin, null).andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("storageKey").doesNotContain("thumb").doesNotContain("athletes/");
    }

    @Test
    void shouldUseTheSameNumberOfSqlStatementsForOneOrTenResults() throws Exception {
        String admin = admin();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        photos(athlete("um." + tag + "@shottrack.com"), 1);
        for (int i = 0; i < 10; i++) {
            photos(athlete("dez." + tag + "." + i + "@shottrack.com"), 1);
        }

        SqlRecorder.clear();
        assertThat(search(admin, "um." + tag)).hasSize(1);
        int forOne = SqlRecorder.statements().size();
        long photoQueriesForOne = SqlRecorder.countContaining("series_photos");

        SqlRecorder.clear();
        assertThat(search(admin, "dez." + tag)).hasSize(10);
        int forTen = SqlRecorder.statements().size();

        assertThat(forTen).as("comandos SQL não crescem com o número de atletas").isEqualTo(forOne);
        assertThat(photoQueriesForOne).as("uso numa só consulta agregada").isEqualTo(1);
        assertThat(SqlRecorder.countContaining("series_photos")).isEqualTo(1);
    }
}
