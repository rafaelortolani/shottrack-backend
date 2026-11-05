package com.shottrack.backend.application.plan;

import com.shottrack.backend.support.PlanApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ADR-0027: as rotas de administração de planos e atletas têm rate limit por administrador (aqui reduzido a 3 por janela). */
@TestPropertySource(properties = "app.rate-limit.admin.attempts=3")
class PlanAdminRateLimitTest extends PlanApiTest {

    @Test
    void shouldRateLimitTheAdminRoutesPerAdministrator() throws Exception {
        String admin = admin();
        for (int i = 0; i < 3; i++) {
            call(HttpMethod.GET, "/api/admin/plans", admin, null).andExpect(status().isOk());
        }

        call(HttpMethod.GET, "/api/admin/plans", admin, null)
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
        call(HttpMethod.PATCH, "/api/admin/plans/FREE/limits", admin, java.util.Map.of("limits", java.util.Map.of("PHOTOS_PER_SERIES", 6)))
                .andExpect(status().isTooManyRequests());
        // outro administrador não é afetado
        call(HttpMethod.GET, "/api/admin/plans", admin(), null).andExpect(status().isOk());
    }

    @Test
    void shouldNotRejectAPlanCodeInUpperCaseInThePathAsAMalformedPath() throws Exception {
        call(HttpMethod.PATCH, "/api/admin/plans/FREE", admin(), java.util.Map.of("name", "Gratuito")).andExpect(status().isOk());
    }
}
