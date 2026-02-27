package com.dvFabricio.VidaLongaFlix.welcome.WelcomeService;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import com.dvFabricio.VidaLongaFlix.services.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class WelcomeServiceTest {

    @InjectMocks
    private WelcomeService welcomeService;

    @Mock
    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(welcomeService, "amandaPhone", "(51) 99999-9999");
        ReflectionTestUtils.setField(welcomeService, "amandaWhatsapp", "https://wa.me/5551999999999");
    }

    @Test
    void shouldSendWelcomeMessage() {
        doNothing().when(whatsAppService).send(any(Message.class));

        welcomeService.sendWelcomeMessage("João Silva", "51999999999");

        // Verifica que o WhatsAppService foi chamado uma vez
        then(whatsAppService).should().send(any(Message.class));
    }

    @Test
    void shouldIncludeNameInMessage() {
        // Captura a mensagem enviada para verificar o conteúdo
        org.mockito.ArgumentCaptor<Message> captor =
                org.mockito.ArgumentCaptor.forClass(Message.class);

        doNothing().when(whatsAppService).send(captor.capture());

        welcomeService.sendWelcomeMessage("Fabricio", "51999999999");

        Message sent = captor.getValue();
        assertTrue(sent.getBody().contains("Fabricio"));
        assertTrue(sent.getBody().contains("VidaLongaFlix"));
        assertTrue(sent.getBody().contains("Amanda Nutri"));
        assertEquals("51999999999", sent.getDestination());
    }

    @Test
    void shouldSendToCorrectPhone() {
        org.mockito.ArgumentCaptor<Message> captor =
                org.mockito.ArgumentCaptor.forClass(Message.class);

        doNothing().when(whatsAppService).send(captor.capture());

        welcomeService.sendWelcomeMessage("João", "(51) 98888-7777");

        assertEquals("(51) 98888-7777", captor.getValue().getDestination());
    }
}