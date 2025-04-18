package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {
    private final WhatsAppService whatsAppService;

    public WelcomeService(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    public void enviarBoasVindas(String nome, String telefone) {
        String corpo = String.format("""
            👋 Olá %s! Seja bem-vindo(a) ao *VidaLongaFlix* 🎬
            ...
        """, nome);
        Message mensagem = new Message(telefone, corpo);
        whatsAppService.enviar(mensagem);
    }
}
