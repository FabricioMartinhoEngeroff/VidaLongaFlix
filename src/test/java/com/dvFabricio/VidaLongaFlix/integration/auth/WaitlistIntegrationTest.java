package com.dvFabricio.VidaLongaFlix.integration.auth;

import com.dvFabricio.VidaLongaFlix.domain.config.AppConfig;
import com.dvFabricio.VidaLongaFlix.domain.user.LoginRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import com.dvFabricio.VidaLongaFlix.repositories.AppConfigRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static com.dvFabricio.VidaLongaFlix.services.RegistrationLimitService.MAX_ACTIVE_USERS_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WaitlistIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppConfigRepository appConfigRepository;

    private String adminToken;

    private static final String ACTIVE_EMAIL = "waitlist-active@test.com";
    private static final String QUEUED_EMAIL = "waitlist-queued@test.com";
    private static final String CANCELLED_EMAIL = "waitlist-cancel@test.com";

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "100"));
    }

    @AfterEach
    void cleanup() {
        List.of(ACTIVE_EMAIL, QUEUED_EMAIL, CANCELLED_EMAIL)
                .forEach(email -> userRepository.findByEmail(email).ifPresent(userRepository::delete));
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "100"));
    }

    @Test
    void shouldQueueRegistrationWhenLimitIsReached() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "1"));

        RegisterRequestDTO request = new RegisterRequestDTO(
                "Fila Usuario",
                QUEUED_EMAIL,
                "Senha@1234",
                "(11) 98765-4321"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queued").value(true))
                .andExpect(jsonPath("$.queuePosition").value(1))
                .andExpect(jsonPath("$.user.status").value("QUEUED"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldBlockLoginWhenUserIsQueued() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "1"));
        registerQueuedUser(QUEUED_EMAIL, "Fila Login");

        LoginRequestDTO request = new LoginRequestDTO(QUEUED_EMAIL, "Senha@1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_QUEUED"))
                .andExpect(jsonPath("$.queuePosition").value(1));
    }

    @Test
    void shouldExposeRegistrationStatusWhenClosed() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "1"));
        registerQueuedUser(QUEUED_EMAIL, "Fila Status");

        mockMvc.perform(get("/auth/registration-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.queueSize").value(1));
    }

    @Test
    void shouldAllowAdminToViewWaitlist() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "1"));
        registerQueuedUser(QUEUED_EMAIL, "Fila Admin");

        mockMvc.perform(bearer(get("/admin/waitlist"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.queue[0].email").value(QUEUED_EMAIL))
                .andExpect(jsonPath("$.queue[0].position").value(1));
    }

    @Test
    void shouldPromoteQueuedUsersWhenAdminIncreasesLimit() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "1"));
        registerQueuedUser(QUEUED_EMAIL, "Fila Promote");

        mockMvc.perform(bearer(put("/admin/config/max-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxActiveUsers\":2}"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxActiveUsers").value(2))
                .andExpect(jsonPath("$.activeUsers").value(2))
                .andExpect(jsonPath("$.promotedFromQueue").value(1));

        LoginRequestDTO login = new LoginRequestDTO(QUEUED_EMAIL, "Senha@1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    void shouldPromoteFirstQueuedUserWhenActiveUserIsDeleted() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "2"));
        registerActiveUser(ACTIVE_EMAIL, "Ativo Teste");
        registerQueuedUser(QUEUED_EMAIL, "Fila Delete");

        String activeUserId = userRepository.findByEmail(ACTIVE_EMAIL).orElseThrow().getId().toString();

        mockMvc.perform(bearer(delete("/users/{id}", activeUserId), adminToken))
                .andExpect(status().isNoContent());

        LoginRequestDTO login = new LoginRequestDTO(QUEUED_EMAIL, "Senha@1234");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    void shouldAllowQueuedUserToCancelOwnEntry() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "1"));
        registerQueuedUser(CANCELLED_EMAIL, "Fila Cancelar");

        mockMvc.perform(delete("/auth/waitlist/me")
                        .param("email", CANCELLED_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Voce foi removido da fila de espera."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(CANCELLED_EMAIL, "Senha@1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectReducingLimitBelowCurrentActiveUsers() throws Exception {
        appConfigRepository.save(new AppConfig(MAX_ACTIVE_USERS_KEY, "2"));
        registerActiveUser(ACTIVE_EMAIL, "Ativo Limite");

        mockMvc.perform(bearer(put("/admin/config/max-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxActiveUsers\":1}"), adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Limite nao pode ser menor que o total de usuarios ativos (2)."));
    }

    private void registerQueuedUser(String email, String name) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequestDTO(
                                name,
                                email,
                                "Senha@1234",
                                "(11) 98765-4321"
                        ))))
                .andExpect(status().isAccepted());
    }

    private void registerActiveUser(String email, String name) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequestDTO(
                                name,
                                email,
                                "Senha@1234",
                                "(11) 98765-4321"
                        ))))
                .andExpect(status().isCreated());
    }
}
