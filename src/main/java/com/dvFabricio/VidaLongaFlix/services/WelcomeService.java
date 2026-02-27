package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {

    private final WhatsAppService whatsAppService;

    // Vem do application.properties — não hardcoded
    @Value("${amanda.phone}")
    private String amandaPhone;

    @Value("${amanda.whatsapp}")
    private String amandaWhatsapp;

    public WelcomeService(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    public void sendWelcomeMessage(String name, String phone) {
        String body = String.format("""
            Olá %s! Bem-vindo(a) ao *VidaLongaFlix*! 🎬🥗
            
            Aqui você encontra os melhores conteúdos sobre
            saúde, nutrição e qualidade de vida!
            
            ---
            
            🌿 *Dica especial para você:*
            Conheça a *Amanda Nutri* - Nutricionista especializada
            em longevidade e alimentação saudável!
            
            📱 Entre em contato: %s
            👉 %s
            
            Cuide da sua saúde com quem entende! 💚
            """, name, amandaPhone, amandaWhatsapp);

        Message message = new Message(phone, body);
        whatsAppService.send(message);
    }
}