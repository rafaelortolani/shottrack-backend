package com.shottrack.backend.application.modality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.user.gateway.repository.PendingRegistrationRepository;
import com.shottrack.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModalityCatalogTest {

    private static final String EMAIL = "atleta.modalidade@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    private String accessToken;

    @BeforeEach
    void registerAndLoginUser() throws Exception {
        accessToken = TestUsers.registerAndLogin(mockMvc, objectMapper, pendingRegistrationRepository,
                "Atleta Modalidade", EMAIL, PASSWORD);
    }

    @Test
    void shouldListModalities() throws Exception {
        mockMvc.perform(get("/api/modality-catalog")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[?(@.name == 'Precisão')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'IPSC')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Steel Challenge')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Trap')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Skeet')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.name == 'Saque e Tiro')]").isNotEmpty());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/modality-catalog"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
