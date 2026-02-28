package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {

    private final WhatsAppService whatsAppService;

    public WelcomeService(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    // O conteúdo da mensagem é definido pelo template aprovado no WhatsApp Manager
    public void sendWelcomeMessage(String name, String phone) {
        Message message = new Message(phone, "welcome_template");
        whatsAppService.send(message);
    }
}
