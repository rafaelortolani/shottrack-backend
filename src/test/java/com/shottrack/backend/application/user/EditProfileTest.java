package com.shottrack.backend.application.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.dto.ConfirmEmailChangeRequest;
import com.shottrack.backend.application.user.dto.UpdateNameRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.user.gateway.repository.EmailVerificationCodeRepository;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EditProfileTest {

    private static final String NAME = "Atleta Perfil";
    private static final String EMAIL = "atleta.editar@shottrack.com";
    private static final String PASSWORD = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    private String accessToken;

    @BeforeEach
    void cadastraELogaUsuario() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest(NAME, EMAIL, PASSWORD))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();
    }

    @Test
    void deveEditarNome() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNameRequest("Novo Nome"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Novo Nome"));
    }

    @Test
    void deveRejeitarNomeVazio() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNameRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deveRejeitarRequisicaoSemToken() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNameRequest("Novo Nome"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void deveTrocarEmailAposConfirmarCodigo() throws Exception {
        String novoEmail = "novo.email@shottrack.com";

        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(novoEmail))))
                .andExpect(status().isAccepted());

        String codigo = codigoPendentePara(novoEmail).getCode();

        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest(codigo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(novoEmail));
    }

    @Test
    void deveRejeitarTrocaParaEmailJaCadastrado() throws Exception {
        String emailExistente = "outro.atleta@shottrack.com";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserRegisterRequest("Outro Atleta", emailExistente, PASSWORD))));

        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(emailExistente))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void deveRejeitarConfirmacaoSemToken() throws Exception {
        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest("123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void deveRejeitarCodigoIncorreto() throws Exception {
        String novoEmail = "codigo.incorreto@shottrack.com";
        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(novoEmail))))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest("000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void deveRejeitarCodigoExpirado() throws Exception {
        String novoEmail = "codigo.expirado@shottrack.com";
        mockMvc.perform(post("/api/users/me/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeEmailRequest(novoEmail))))
                .andExpect(status().isAccepted());

        EmailVerificationCode pendente = codigoPendentePara(novoEmail);
        ReflectionTestUtils.setField(pendente, "expiresAt", Instant.now().minusSeconds(1));
        emailVerificationCodeRepository.saveAndFlush(pendente);

        mockMvc.perform(post("/api/users/me/email/confirmation")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailChangeRequest(pendente.getCode()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VERIFICATION_CODE_EXPIRED"));
    }

    private EmailVerificationCode codigoPendentePara(String novoEmail) {
        return emailVerificationCodeRepository.findAll().stream()
                .filter(v -> v.getNewEmail().equals(novoEmail) && !v.isUsed())
                .findFirst()
                .orElseThrow();
    }
}
