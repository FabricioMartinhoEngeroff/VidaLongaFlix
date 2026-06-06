package com.dvFabricio.VidaLongaFlix.emailTest.service;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.services.EmailService;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class WelcomeServiceTest {

    @InjectMocks
    private WelcomeService welcomeService;

    @Mock
    private EmailService emailService;

    // WS-01 — deve chamar EmailService ao enviar boas-vindas
    @Test
    void shouldCallEmailServiceWhenSendingWelcome() {
        doNothing().when(emailService).send(any(EmailMessage.class));

        welcomeService.sendWelcomeMessage("João Silva", "joao@email.com");

        then(emailService).should().send(any(EmailMessage.class));
    }

    // WS-02 — deve enviar para o email correto do usuário
    @Test
    void shouldSendToCorrectEmail() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);

        welcomeService.sendWelcomeMessage("Maria", "maria@teste.com");

        then(emailService).should().send(captor.capture());
        assertEquals("maria@teste.com", captor.getValue().to());
    }

    // WS-03 — deve incluir o nome do usuário no assunto
    @Test
    void shouldIncludeUserNameInSubject() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);

        welcomeService.sendWelcomeMessage("Carlos", "carlos@email.com");

        then(emailService).should().send(captor.capture());
        assertTrue(captor.getValue().subject().contains("Carlos"));
    }

    // WS-04 — deve incluir o nome do usuário no corpo
    @Test
    void shouldIncludeUserNameInBody() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);

        welcomeService.sendWelcomeMessage("Ana", "ana@email.com");

        then(emailService).should().send(captor.capture());
        assertTrue(captor.getValue().body().contains("Ana"));
    }

    // WS-05 — deve incluir o link da plataforma no corpo
    @Test
    void shouldIncludePlatformLinkInBody() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);

        welcomeService.sendWelcomeMessage("Lucas", "lucas@email.com");

        then(emailService).should().send(captor.capture());
        assertTrue(captor.getValue().body().contains("vidalongaflix.com"));
    }

    // WS-06 — deve incluir referência à nutricionista no corpo
    @Test
    void shouldIncludeNutritionistNameInBody() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);

        welcomeService.sendWelcomeMessage("Fernanda", "fernanda@email.com");

        then(emailService).should().send(captor.capture());
        assertTrue(captor.getValue().body().contains("Amanda"));
    }

    // WS-07 — não deve propagar exceção se EmailService falhar (best-effort)
    @Test
    void shouldNotPropagateExceptionWhenEmailServiceFails() {
        doThrow(new RuntimeException("SMTP error")).when(emailService).send(any(EmailMessage.class));

        assertDoesNotThrow(() -> welcomeService.sendWelcomeMessage("João", "joao@email.com"));
    }

    // WS-08 — não deve aceitar email nulo
    @Test
    void shouldThrowWhenEmailIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> welcomeService.sendWelcomeMessage("João", null));
    }

    // WS-09 — não deve aceitar nome em branco
    @Test
    void shouldThrowWhenNameIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> welcomeService.sendWelcomeMessage("", "joao@email.com"));
    }
}
