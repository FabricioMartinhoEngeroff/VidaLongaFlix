package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {

    @Value("${whatsapp.template.name:welcome_template}")
    private String welcomeTemplateName = "welcome_template";

    private final WhatsAppService whatsAppService;

    public WelcomeService(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    // O conteúdo da mensagem é definido pelo template aprovado no WhatsApp Manager
    public void sendWelcomeMessage(String name, String phone) {
        Message message = new Message(phone, welcomeTemplateName);
        whatsAppService.send(message);
    }
}
