package com.dvFabricio.VidaLongaFlix.welcome.whatsappTest;

import com.dvFabricio.VidaLongaFlix.services.WelcomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WhatsAppIntegrationTest {

    @Autowired
    private WelcomeService welcomeService;

    @Test
    void shouldSendRealWelcomeMessage() {
        // Coloca seu número real aqui — precisa estar conectado no sandbox
        welcomeService.sendWelcomeMessage(
                "Fabricio",
                "+5551996407776"// <- seu número real sem formatação
        );

        // Sem assert — só verifica se não lança exceção
        // Confirma recebendo o WhatsApp no celular
    }
}