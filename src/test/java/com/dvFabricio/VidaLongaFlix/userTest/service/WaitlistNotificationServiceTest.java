package com.dvFabricio.VidaLongaFlix.userTest.service;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.services.EmailService;
import com.dvFabricio.VidaLongaFlix.services.WaitlistNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class WaitlistNotificationServiceTest {

    @InjectMocks
    private WaitlistNotificationService waitlistNotificationService;

    @Mock
    private EmailService emailService;

    // WN-01 — notifyQueued deve enviar email com posição na fila
    @Test
    void shouldSendQueueEmailWithPositionWhenUserIsAddedToWaitlist() {
        User user = new User("Fila Usuario", "fila@test.com", "pwd", "(11) 98765-4321");
        user.setQueuePosition(3);

        waitlistNotificationService.notifyQueued(user);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(emailService).should().send(captor.capture());

        EmailMessage email = captor.getValue();
        assertEquals("fila@test.com", email.to());
        assertEquals("VidaLongaFlix - Fila de espera", email.subject());
        assertTrue(email.body().contains("fila de espera"));
        assertTrue(email.body().contains("#3"));
    }

    // WN-02 — notifyActivated deve enviar email e NÃO enviar WhatsApp
    @Test
    void shouldSendActivationEmailOnlyWithoutWhatsApp() {
        User user = new User("Ativado Usuario", "ativo@test.com", "pwd", "(11) 98765-4321");

        waitlistNotificationService.notifyActivated(user);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(emailService).should().send(captor.capture());

        EmailMessage email = captor.getValue();
        assertEquals("ativo@test.com", email.to());
        assertEquals("VidaLongaFlix - Conta ativada", email.subject());
        assertTrue(email.body().contains("foi ativada"));
    }

    // WN-03 — notifyRemoved deve enviar email ao usuário removido
    @Test
    void shouldSendRemovalEmailWhenUserLeavesWaitlist() {
        User user = new User("Pedro", "pedro@email.com", "pwd", "(11) 98765-4321");

        waitlistNotificationService.notifyRemoved(user);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(emailService).should().send(captor.capture());

        EmailMessage email = captor.getValue();
        assertEquals("pedro@email.com", email.to());
        assertEquals("VidaLongaFlix - Saida da fila de espera", email.subject());
        assertTrue(email.body().contains("removido"));
    }

    // WN-04 — notifyQueued não deve propagar exceção se EmailService falhar
    @Test
    void shouldNotPropagateExceptionWhenQueueEmailFails() {
        User user = new User("Usuario", "user@test.com", "pwd", "(11) 98765-4321");
        user.setQueuePosition(1);
        doThrow(new RuntimeException("SMTP error")).when(emailService).send(any(EmailMessage.class));

        assertDoesNotThrow(() -> waitlistNotificationService.notifyQueued(user));
    }

    // WN-05 — notifyActivated não deve propagar exceção se EmailService falhar
    @Test
    void shouldNotPropagateExceptionWhenActivationEmailFails() {
        User user = new User("Usuario", "user@test.com", "pwd", "(11) 98765-4321");
        doThrow(new RuntimeException("SMTP error")).when(emailService).send(any(EmailMessage.class));

        assertDoesNotThrow(() -> waitlistNotificationService.notifyActivated(user));
    }
}
