package com.dvFabricio.VidaLongaFlix.welcome.WelcomeService;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import com.dvFabricio.VidaLongaFlix.services.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class WelcomeServiceTest {

    @InjectMocks
    private WelcomeService welcomeService;

    @Mock
    private WhatsAppService whatsAppService;

    // Não há mais @BeforeEach com amandaPhone/amandaWhatsapp
    // pois o conteúdo agora é gerenciado pelo template da Meta

    @Test
    void shouldCallWhatsAppServiceWhenSendingWelcome() {
        doNothing().when(whatsAppService).send(any(Message.class));

        welcomeService.sendWelcomeMessage("João Silva", "51999999999");

        // Verifica que o WhatsAppService foi chamado uma vez
        then(whatsAppService).should().send(any(Message.class));
    }

    @Test
    void shouldSendToCorrectPhone() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(whatsAppService).send(captor.capture());

        welcomeService.sendWelcomeMessage("Fabricio", "(51) 98888-7777");

        assertEquals("(51) 98888-7777", captor.getValue().getDestination());
    }

    @Test
    void shouldUseApprovedWelcomeTemplate() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(whatsAppService).send(captor.capture());

        welcomeService.sendWelcomeMessage("Fabricio", "(51) 98888-7777");

        assertEquals("welcome_template", captor.getValue().getBody());
    }
}
