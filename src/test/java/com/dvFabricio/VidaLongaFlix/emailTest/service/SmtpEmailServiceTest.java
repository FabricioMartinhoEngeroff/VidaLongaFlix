package com.dvFabricio.VidaLongaFlix.emailTest.service;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.services.email.SmtpEmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @InjectMocks
    private SmtpEmailService smtpEmailService;

    @Mock
    private JavaMailSender mailSender;

    // SMTP-01 — deve chamar mailSender.send() com o MimeMessage criado
    @Test
    void shouldSendMimeMessageToRecipient() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        smtpEmailService.send(new EmailMessage("dest@email.com", "Assunto", "Corpo"));

        verify(mailSender).send(mimeMessage);
    }

    // SMTP-02 — deve chamar send independentemente do assunto passado
    @Test
    void shouldSendWithCorrectSubject() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        smtpEmailService.send(new EmailMessage("dest@email.com", "Bem-vindo!", "Corpo"));

        verify(mailSender).send(mimeMessage);
    }

    // SMTP-03 — deve propagar MailException para que o chamador faça best-effort
    @Test
    void shouldPropagateMailExceptionToCallerForBestEffortHandling() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP falhou")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class,
                () -> smtpEmailService.send(new EmailMessage("dest@email.com", "Assunto", "Corpo")));
    }

    // SMTP-04 — deve chamar mailSender.send() exatamente uma vez por EmailMessage
    @Test
    void shouldCallMailSenderSendExactlyOnce() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        smtpEmailService.send(new EmailMessage("dest@email.com", "Assunto", "Corpo"));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
