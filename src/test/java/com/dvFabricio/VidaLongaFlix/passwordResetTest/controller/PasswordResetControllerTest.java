package com.dvFabricio.VidaLongaFlix.passwordResetTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.PasswordResetController;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.TokenExpiredException;
import com.dvFabricio.VidaLongaFlix.services.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PasswordResetController controller;

    @Mock
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setup() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void requestReset_shouldReturn200ForValidEmail() throws Exception {
        willDoNothing().given(passwordResetService).requestReset(anyString());

        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"joao@teste.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void requestReset_shouldReturn200EvenForUnknownEmail() throws Exception {
        willDoNothing().given(passwordResetService).requestReset(anyString());

        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"naoexiste@teste.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void requestReset_shouldReturn400ForInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nao-e-um-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateToken_shouldReturn204ForValidToken() throws Exception {
        willDoNothing().given(passwordResetService).validateToken("tokenvalido");

        mockMvc.perform(get("/auth/validate-token")
                        .param("token", "tokenvalido"))
                .andExpect(status().isNoContent());
    }

    @Test
    void validateToken_shouldReturn410ForExpiredToken() throws Exception {
        willThrow(new TokenExpiredException("Token expirado."))
                .given(passwordResetService).validateToken("tokenexpirado");

        mockMvc.perform(get("/auth/validate-token")
                        .param("token", "tokenexpirado"))
                .andExpect(status().isGone());
    }

    @Test
    void validateToken_shouldReturn404ForUnknownToken() throws Exception {
        willThrow(new ResourceNotFoundExceptions("Token não encontrado."))
                .given(passwordResetService).validateToken("tokeninexistente");

        mockMvc.perform(get("/auth/validate-token")
                        .param("token", "tokeninexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resetPassword_shouldReturn204OnSuccess() throws Exception {
        willDoNothing().given(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"tokenvalido\", \"newPassword\": \"NovaSenha@123\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetPassword_shouldReturn410ForInvalidToken() throws Exception {
        willThrow(new TokenExpiredException("Token expirado ou já utilizado."))
                .given(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"tokenexpirado\", \"newPassword\": \"NovaSenha@123\"}"))
                .andExpect(status().isGone());
    }

    @Test
    void resetPassword_shouldReturn400ForWeakPassword() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"tokenvalido\", \"newPassword\": \"123\"}"))
                .andExpect(status().isBadRequest());
    }
}