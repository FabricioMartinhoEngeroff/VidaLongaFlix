package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {
    private final WhatsAppService whatsAppService;

    private static final String AMANDA_PHONE = "(XX) XXXXX-XXXX";

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
            👉 https://wa.me/%s
            
            Cuide da sua saúde com quem entende! 💚
            """, name, AMANDA_PHONE, AMANDA_PHONE.replaceAll("[^0-9]", ""));

        Message message = new Message(phone, body);
        whatsAppService.send(message);
    }
}
