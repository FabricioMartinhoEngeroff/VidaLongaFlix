package com.dvFabricio.VidaLongaFlix.passwordResetTest.service;

import com.dvFabricio.VidaLongaFlix.domain.passwordreset.PasswordResetToken;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.TokenExpiredException;
import com.dvFabricio.VidaLongaFlix.repositories.PasswordResetTokenRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.PasswordResetEmailService;
import com.dvFabricio.VidaLongaFlix.services.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @InjectMocks
    private PasswordResetService service;

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordResetEmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    private User activeUser;
    private User disabledUser;
    private User queuedUser;
    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();

        activeUser = new User();
        activeUser.setName("João Ativo");
        activeUser.setEmail("joao@teste.com");
        activeUser.setPassword("hashed");
        activeUser.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(activeUser, "id", userId);

        disabledUser = new User();
        disabledUser.setName("Maria Desativada");
        disabledUser.setEmail("maria@teste.com");
        disabledUser.setPassword("hashed");
        disabledUser.setStatus(UserStatus.DISABLED);

        queuedUser = new User();
        queuedUser.setName("Pedro Fila");
        queuedUser.setEmail("pedro@teste.com");
        queuedUser.setPassword("hashed");
        queuedUser.setStatus(UserStatus.QUEUED);
    }

    @Test
    void requestReset_shouldSaveTokenForActiveUser() {
        given(userRepository.findByEmail("joao@teste.com")).willReturn(Optional.of(activeUser));

        service.requestReset("joao@teste.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        then(tokenRepository).should().save(captor.capture());

        PasswordResetToken saved = captor.getValue();
        assertAll(
                () -> assertEquals(64, saved.getToken().length(), "Token deve ter 64 caracteres"),
                () -> assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()), "Token deve expirar no futuro"),
                () -> assertFalse(saved.isUsed(), "Token recém-criado não deve estar marcado como usado")
        );
    }

    @Test
    void requestReset_shouldDeleteOldTokenBeforeSavingNew() {
        given(userRepository.findByEmail("joao@teste.com")).willReturn(Optional.of(activeUser));

        service.requestReset("joao@teste.com");

        var inOrder = inOrder(tokenRepository);
        inOrder.verify(tokenRepository).deleteByUserId(userId);
        inOrder.verify(tokenRepository).save(any());
    }

    @Test
    void requestReset_shouldReturnSilentlyForUnknownEmail() {
        given(userRepository.findByEmail("naoexiste@teste.com")).willReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requestReset("naoexiste@teste.com"));

        then(tokenRepository).should(never()).save(any());
        then(emailService).should(never()).sendResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void requestReset_shouldReturnSilentlyForDisabledUser() {
        given(userRepository.findByEmail("maria@teste.com")).willReturn(Optional.of(disabledUser));

        assertDoesNotThrow(() -> service.requestReset("maria@teste.com"));

        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void requestReset_shouldReturnSilentlyForQueuedUser() {
        given(userRepository.findByEmail("pedro@teste.com")).willReturn(Optional.of(queuedUser));

        assertDoesNotThrow(() -> service.requestReset("pedro@teste.com"));

        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void validateToken_shouldNotThrowForValidToken() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "tokenvalido", LocalDateTime.now().plusMinutes(30));
        given(tokenRepository.findByToken("tokenvalido")).willReturn(Optional.of(token));

        assertDoesNotThrow(() -> service.validateToken("tokenvalido"));
    }

    @Test
    void validateToken_shouldThrowForUnknownToken() {
        given(tokenRepository.findByToken("inexistente")).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class, () -> service.validateToken("inexistente"));
    }

    @Test
    void validateToken_shouldThrowForExpiredToken() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "expirado", LocalDateTime.now().minusMinutes(1));
        given(tokenRepository.findByToken("expirado")).willReturn(Optional.of(token));

        assertThrows(TokenExpiredException.class, () -> service.validateToken("expirado"));
    }

    @Test
    void validateToken_shouldThrowForUsedToken() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "usado", LocalDateTime.now().plusMinutes(30));
        token.setUsed(true);
        given(tokenRepository.findByToken("usado")).willReturn(Optional.of(token));

        assertThrows(TokenExpiredException.class, () -> service.validateToken("usado"));
    }

    @Test
    void resetPassword_shouldSaveHashedPassword() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "tokenok", LocalDateTime.now().plusMinutes(30));
        given(tokenRepository.findByToken("tokenok")).willReturn(Optional.of(token));
        given(passwordEncoder.encode("NovaSenha@123")).willReturn("$2a$hashed");

        service.resetPassword("tokenok", "NovaSenha@123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());

        String savedPassword = userCaptor.getValue().getPassword();
        assertEquals("$2a$hashed", savedPassword, "Senha deve estar hasheada com BCrypt");
        assertNotEquals("NovaSenha@123", savedPassword, "Senha em texto claro nunca deve ser salva");
    }

    @Test
    void resetPassword_shouldMarkTokenAsUsed() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "tokenok", LocalDateTime.now().plusMinutes(30));
        given(tokenRepository.findByToken("tokenok")).willReturn(Optional.of(token));
        given(passwordEncoder.encode(anyString())).willReturn("$2a$hashed");

        service.resetPassword("tokenok", "NovaSenha@123");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        then(tokenRepository).should().save(tokenCaptor.capture());

        assertTrue(tokenCaptor.getValue().isUsed(), "Token deve ser marcado como usado");
    }

    @Test
    void resetPassword_shouldSendConfirmationEmail() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "tokenok", LocalDateTime.now().plusMinutes(30));
        given(tokenRepository.findByToken("tokenok")).willReturn(Optional.of(token));
        given(passwordEncoder.encode(anyString())).willReturn("$2a$hashed");

        service.resetPassword("tokenok", "NovaSenha@123");

        then(emailService).should().sendChangeConfirmation(activeUser.getName(), activeUser.getEmail());
    }

    @Test
    void resetPassword_shouldNotRollbackIfConfirmationEmailFails() {
        PasswordResetToken token = new PasswordResetToken(activeUser, "tokenok", LocalDateTime.now().plusMinutes(30));
        given(tokenRepository.findByToken("tokenok")).willReturn(Optional.of(token));
        given(passwordEncoder.encode(anyString())).willReturn("$2a$hashed");

        willThrow(new RuntimeException("SMTP falhou"))
                .given(emailService).sendChangeConfirmation(anyString(), anyString());

        assertDoesNotThrow(() -> service.resetPassword("tokenok", "NovaSenha@123"));

        then(userRepository).should().save(any(User.class));
    }
}
