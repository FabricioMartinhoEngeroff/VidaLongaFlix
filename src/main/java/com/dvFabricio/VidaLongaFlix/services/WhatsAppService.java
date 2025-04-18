package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.Map;

@Service
public class WhatsAppService {

    private final String token = "SEU_ACCESS_TOKEN";             // Token do Meta Developer
    private final String phoneNumberId = "SEU_PHONE_NUMBER_ID";  // ID do número remetente

    @Async
    public void enviar(Message message){
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", message.getDestino(),
                "type", "text",
                "text", Map.of("body", message.getCorpo())
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);                       // Token JWT do app
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

        try {
            restTemplate.postForEntity(url, request, String.class);
            message.setStatusEntrega("ENVIADO");
            System.out.println("WhatsApp enviado com sucesso para " + message.getDestino());
        } catch (Exception e) {
            message.setStatusEntrega("FALHA");
            throw new ResourceAccessException("Falha ao enviar mensagem via WhatsApp: " + e.getMessage(), e);
        }
    }
}
