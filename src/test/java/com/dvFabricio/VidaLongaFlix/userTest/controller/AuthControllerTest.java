package com.dvFabricio.VidaLongaFlix.userTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.AuthController;
import com.dvFabricio.VidaLongaFlix.domain.user.LoginRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.RegisterRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AuthController authController;
    @Mock private UserRepository repository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private WelcomeService welcomeService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        user = new User("João Silva", "joao@example.com", "encodedPassword", "(11) 99999-9999");
        user.setId(UUID.randomUUID());

        role = new Role("ROLE_USER");
        role.setId(UUID.randomUUID());
        user.setRoles(List.of(role));
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
                .andExpect(jsonPath("$.user.name").value("João Silva"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordWrong() throws Exception {
        when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1@", "encodedPassword")).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO("joao@example.com", "WrongPass1@");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserNotFound() throws Exception {
        when(repository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO("notfound@example.com", "Password1@");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        when(repository.existsByEmail("joao@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", UUID.randomUUID());
            return savedUser;
        });
        when(tokenService.generateToken(any(User.class))).thenReturn("mockToken");
        doNothing().when(welcomeService).sendWelcomeMessage(any(), any());

        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mockToken"));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        when(repository.existsByEmail("joao@example.com")).thenReturn(true);

        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnBadRequestWhenPasswordTooWeak() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva", "joao@example.com", "fraca", "(11) 99999-9999");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                )
        );

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void shouldReturnNotFoundWhenUserNotAuthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isNotFound());
    }

}