package com.dvFabricio.VidaLongaFlix.integration.auth;

import com.dvFabricio.VidaLongaFlix.domain.user.LoginRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o fluxo de autenticação completo.
 * Cobre login, registro, validações e recuperação do usuário autenticado.
 */
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    // Email usado nos testes de registro — removido após cada teste
    private static final String NEW_USER_EMAIL = "integration-new@vidalongaflix.test";

    @AfterEach
    void cleanup() {
        userRepository.findByEmail(NEW_USER_EMAIL)
                .ifPresent(userRepository::delete);
    }

    // ─────────────────────────── LOGIN ────────────────────────────────────

    @Test
    void shouldLoginWithAdminCredentials() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(
                ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.user.roles").isArray());
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(
                "admin@vidalongaflix.com", "SenhaErrada@1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldReturnUnauthorizedWhenEmailDoesNotExist() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO(
                "naoexiste@vidalongaflix.com", "Qualquer@123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestForLoginWithBlankEmail() throws Exception {
        String body = "{\"email\":\"\",\"password\":\"" + ADMIN_PASSWORD + "\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForLoginWithInvalidEmailFormat() throws Exception {
        String body = "{\"email\":\"email-invalido\",\"password\":\"" + ADMIN_PASSWORD + "\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── REGISTRO ─────────────────────────────────

    @Test
    void shouldRegisterNewUserSuccessfully() throws Exception {
        // NOTA: este teste documentará falha se o endpoint de registro não definir
        // taxId antes de salvar (campo NOT NULL no banco). Ver DataInitializer como referência.
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Integration User",
                NEW_USER_EMAIL,
                "Senha@1234",
                "(11) 99999-9999"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(NEW_USER_EMAIL))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_USER"));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        // O admin já foi criado pelo DataInitializer — tentar registrar o mesmo email é conflito
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Admin Duplicado",
                "admin@vidalongaflix.com",
                "OutraSenha@1",
                "(11) 99999-9999"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate resource"))
                .andExpect(jsonPath("$.message").value("Email is already in use."));
    }

    @Test
    void shouldReturnBadRequestForWeakPassword() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Usuario Fraco",
                "fraco@vidalongaflix.test",
                "fraca",           // sem maiúscula, número e símbolo
                "(11) 99999-9999"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidPhoneFormat() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Fone Errado",
                "fone@vidalongaflix.test",
                "Senha@1234",
                "99999-9999"        // falta o (DD)
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingName() throws Exception {
        String body = "{\"name\":\"\",\"email\":\"sem-nome@test.com\","
                + "\"password\":\"Senha@1234\",\"phone\":\"(11) 99999-9999\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'name')]").exists());
    }

    @Test
    void shouldReturnDetailedValidationErrorsForInvalidRegisterPayload() throws Exception {
        String body = """
                {
                  "name": "",
                  "email": "email-invalido",
                  "password": "fraca",
                  "phone": "11999999999"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'phone')]").exists());
    }

    @Test
    void shouldReturnBadRequestForMalformedJsonOnRegister() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integration User\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    // ─────────────────────────── /auth/me ─────────────────────────────────

    @Test
    void shouldReturnAuthenticatedUserWithValidToken() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(bearer(get("/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@vidalongaflix.com"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void shouldReturnNotFoundForMeWithoutToken() throws Exception {
        // /auth/me é permitAll mas o controller lança ResourceNotFoundExceptions se user == null
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }

    @Test
    void shouldReturnNotFoundForMeWithInvalidToken() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }
}
