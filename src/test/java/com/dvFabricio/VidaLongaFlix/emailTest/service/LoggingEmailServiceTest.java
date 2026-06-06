package com.dvFabricio.VidaLongaFlix.emailTest.service;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.services.LoggingEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class LoggingEmailServiceTest {

    @InjectMocks
    private LoggingEmailService loggingEmailService;

    // LOG-01 — deve logar sem lançar exceção
    @Test
    void shouldNotThrowWhenLoggingEmail() {
        assertDoesNotThrow(() ->
                loggingEmailService.send(new EmailMessage("a@b.com", "Assunto", "Corpo")));
    }

    // LOG-02 — não deve lançar exceção para nenhum EmailMessage válido
    @Test
    void shouldNotThrowForAnyValidEmailMessage() {
        assertDoesNotThrow(() ->
                loggingEmailService.send(new EmailMessage("x@y.com", "S", "B")));
    }
}
