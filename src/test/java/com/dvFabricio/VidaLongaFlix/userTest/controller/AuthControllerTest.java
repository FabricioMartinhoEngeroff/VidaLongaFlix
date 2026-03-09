package com.dvFabricio.VidaLongaFlix.userTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.AuthController;
import com.dvFabricio.VidaLongaFlix.domain.auth.QueueLoginErrorDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.auth.RegistrationStatusDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.LoginRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.domain.waitlist.WaitlistMessageDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.RegistrationLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AuthController authController;
    @Mock private UserRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private RegistrationLimitService registrationLimitService;

    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        user = new User("João Silva", "joao@example.com", "encodedPassword", "(11) 99999-9999");
        user.setId(UUID.randomUUID());
        user.setRoles(List.of(new Role("ROLE_USER")));
        user.setStatus(UserStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1@", "encodedPassword")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("mockToken");

        LoginRequestDTO request = new LoginRequestDTO("joao@example.com", "Password1@");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mockToken"))
                .andExpect(jsonPath("$.user.email").value("joao@example.com"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordWrong() throws Exception {
        when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1@", "encodedPassword")).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO("joao@example.com", "WrongPass1@");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(tokenService, never()).generateToken(user);
    }

    @Test
    void shouldReturnQueuedPayloadWhenQueuedUserTriesToLogin() throws Exception {
        user.setStatus(UserStatus.QUEUED);
        user.setQueuePosition(3);

        when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));
        when(registrationLimitService.buildQueuedLoginError(user)).thenReturn(
                new QueueLoginErrorDTO("ACCOUNT_QUEUED", "Sua conta esta na fila de espera.", 3)
        );

        LoginRequestDTO request = new LoginRequestDTO("joao@example.com", "Password1@");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_QUEUED"))
                .andExpect(jsonPath("$.queuePosition").value(3));
    }

    @Test
    void shouldReturnDisabledPayloadWhenDisabledUserTriesToLogin() throws Exception {
        user.setStatus(UserStatus.DISABLED);

        when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));
        when(registrationLimitService.buildDisabledLoginError()).thenReturn(
                new QueueLoginErrorDTO("ACCOUNT_DISABLED", "Sua conta foi desativada.", null)
        );

        LoginRequestDTO request = new LoginRequestDTO("joao@example.com", "Password1@");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
    }

    @Test
    void shouldReturnDetailedValidationErrorsWhenLoginPayloadIsInvalid() throws Exception {
        String body = """
                {
                  "email": "email-invalido",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'password')]").exists());
    }

    @Test
    void shouldReturnBadRequestWhenLoginPayloadIsMalformedJson() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@example.com\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    void shouldReturnAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void shouldReturnNotFoundWhenUserNotAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }

    @Test
    void shouldReturnCreatedWhenRegistrationCreatesActiveUser() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");

        RegistrationResponseDTO response = new RegistrationResponseDTO(
                "mockToken",
                new UserResponseDTO(user),
                false,
                null,
                null
        );

        when(registrationLimitService.register(request)).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queued").value(false))
                .andExpect(jsonPath("$.token").value("mockToken"));
    }

    @Test
    void shouldReturnAcceptedWhenRegistrationQueuesUser() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");

        user.setStatus(UserStatus.QUEUED);
        user.setQueuePosition(5);

        RegistrationResponseDTO response = new RegistrationResponseDTO(
                null,
                new UserResponseDTO(user),
                true,
                5,
                "Limite de usuarios atingido. Voce foi adicionado a fila de espera na posicao #5."
        );

        when(registrationLimitService.register(request)).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queued").value(true))
                .andExpect(jsonPath("$.queuePosition").value(5))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturnRegistrationStatus() throws Exception {
        when(registrationLimitService.getRegistrationStatus())
                .thenReturn(new RegistrationStatusDTO(false, 100, 100, 4));

        mockMvc.perform(get("/auth/registration-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.activeUsers").value(100))
                .andExpect(jsonPath("$.queueSize").value(4));
    }

    @Test
    void shouldCancelOwnWaitlistEntryByEmail() throws Exception {
        when(registrationLimitService.removeQueuedUserByEmail("joao@example.com"))
                .thenReturn(new WaitlistMessageDTO("Voce foi removido da fila de espera."));

        mockMvc.perform(delete("/auth/waitlist/me")
                        .param("email", "joao@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Voce foi removido da fila de espera."));
    }
}
