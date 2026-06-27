package com.dvFabricio.VidaLongaFlix.passwordResetTest.service;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.services.email.EmailService;
import com.dvFabricio.VidaLongaFlix.services.password.PasswordResetEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTest {

    @InjectMocks
    private PasswordResetEmailService emailService;

    @Mock
    private EmailService delegateEmailService;

    @Test
    void sendResetEmail_shouldSendToCorrectRecipient() {
        emailService.sendResetEmail("João", "joao@teste.com", "tokenxyz");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(delegateEmailService).should().send(captor.capture());

        assertEquals("joao@teste.com", captor.getValue().to());
    }

    @Test
    void sendResetEmail_shouldIncludeLinkWithToken() {
        emailService.sendResetEmail("João", "joao@teste.com", "meutoken123");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(delegateEmailService).should().send(captor.capture());

        assertTrue(
                captor.getValue().body().contains("password-change?token=meutoken123"),
                "Corpo deve conter o link com o token"
        );
    }

    @Test
    void sendResetEmail_shouldIncludeUserName() {
        emailService.sendResetEmail("Maria", "maria@teste.com", "token");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(delegateEmailService).should().send(captor.capture());

        assertTrue(captor.getValue().body().contains("Maria"));
    }

    @Test
    void sendResetEmail_shouldPropagateExceptionOnFailure() {
        willThrow(new RuntimeException("SMTP offline"))
                .given(delegateEmailService).send(any());

        assertThrows(RuntimeException.class,
                () -> emailService.sendResetEmail("João", "joao@teste.com", "token"));
    }


    @Test
    void sendChangeConfirmation_shouldSendToCorrectRecipient() {
        emailService.sendChangeConfirmation("Carlos", "carlos@teste.com");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(delegateEmailService).should().send(captor.capture());

        assertEquals("carlos@teste.com", captor.getValue().to());
    }

    @Test
    void sendChangeConfirmation_shouldNotContainToken() {
        emailService.sendChangeConfirmation("Carlos", "carlos@teste.com");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(delegateEmailService).should().send(captor.capture());

        assertFalse(captor.getValue().body().contains("token"));
    }

    @Test
    void sendChangeConfirmation_shouldNotPropagateException() {
        willThrow(new RuntimeException("SMTP offline"))
                .given(delegateEmailService).send(any());

        assertDoesNotThrow(() -> emailService.sendChangeConfirmation("Carlos", "carlos@teste.com"));
    }
}
