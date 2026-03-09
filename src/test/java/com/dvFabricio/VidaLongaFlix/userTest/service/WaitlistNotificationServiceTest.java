package com.dvFabricio.VidaLongaFlix.userTest.service;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.services.EmailService;
import com.dvFabricio.VidaLongaFlix.services.WaitlistNotificationService;
import com.dvFabricio.VidaLongaFlix.services.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WaitlistNotificationServiceTest {

    @InjectMocks
    private WaitlistNotificationService waitlistNotificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private WhatsAppService whatsAppService;

    @Test
    void shouldSendQueueEmailWhenUserIsAddedToWaitlist() {
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

        then(whatsAppService).should(never()).send(any(Message.class));
    }

    @Test
    void shouldSendActivationEmailAndWhatsAppWhenUserGetsActivated() {
        User user = new User("Ativado Usuario", "ativo@test.com", "pwd", "(11) 98765-4321");

        waitlistNotificationService.notifyActivated(user);

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        then(emailService).should().send(emailCaptor.capture());

        EmailMessage email = emailCaptor.getValue();
        assertEquals("ativo@test.com", email.to());
        assertEquals("VidaLongaFlix - Conta ativada", email.subject());
        assertTrue(email.body().contains("foi ativada"));

        ArgumentCaptor<Message> whatsAppCaptor = ArgumentCaptor.forClass(Message.class);
        then(whatsAppService).should().send(whatsAppCaptor.capture());
        assertEquals("(11) 98765-4321", whatsAppCaptor.getValue().getDestination());
        assertEquals("account_activated_template", whatsAppCaptor.getValue().getBody());
    }

    @Test
    void shouldSendRemovalEmailWhenUserLeavesWaitlist() {
        User user = new User("Removido Usuario", "removido@test.com", "pwd", "(11) 98765-4321");

        waitlistNotificationService.notifyRemoved(user);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(emailService).should().send(captor.capture());

        EmailMessage email = captor.getValue();
        assertEquals("removido@test.com", email.to());
        assertEquals("VidaLongaFlix - Saida da fila de espera", email.subject());
        assertTrue(email.body().contains("removido"));

        then(whatsAppService).should(never()).send(any(Message.class));
    }
}
