package com.dvFabricio.VidaLongaFlix.userTest.controller;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.security.TokenService;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTestMoclMVC {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private TokenService tokenService;

    @Test
    void shouldRegisterUser_WhenValidInput() throws Exception {
        String email = "newuser@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";
        String login = "newUser";
        String token = "mockToken";

        Role role = new Role("ROLE_USER");
        role.setId(UUID.randomUUID());

        User newUser = new User(login, email, encodedPassword);
        newUser.setId(UUID.randomUUID());
        newUser.setRoles(List.of(role));

        Mockito.when(userRepository.existsByEmail(email)).thenReturn(false);
        Mockito.when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(newUser);
        Mockito.when(tokenService.generateToken(Mockito.any(User.class))).thenReturn(token);

        String requestBody = """
                {
                    "login": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(login, email, password);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    void shouldNotRegisterUser_WhenEmailAlreadyExists() throws Exception {

        Mockito.when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        String requestBody = """
                {
                    "login": "user",
                    "email": "existing@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is already in use."));
    }

    @Test
    void shouldLoginUser_WhenValidCredentials() throws Exception {

        String email = "user@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";
        String token = "mockToken";

        User user = new User("user", email, encodedPassword);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        Mockito.when(tokenService.generateToken(Mockito.any(User.class))).thenReturn(token);

        String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("user"))
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    void shouldReturnUnauthorized_WhenInvalidPassword() throws Exception {
        // Dados simulados
        String email = "user@example.com";
        String password = "wrongPassword";
        String encodedPassword = "encodedPassword123";

        User user = new User("user", email, encodedPassword);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

        String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

}
