package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.domain.message.DeliveryStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from}")
    private String fromNumber;

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    // @Async removido temporariamente para debug
    // O teste terminava antes da thread async completar
    public void send(Message message) {
        if (!enabled) {
            System.out.println("[DEV] WhatsApp simulado para: " + message.getDestination());
            message.setDeliveryStatus(DeliveryStatus.SENT);
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        String toNumber = "whatsapp:+" + message.getDestination().replaceAll("[^0-9]", "");
        String fromWhatsApp = "whatsapp:" + fromNumber;

        System.out.println("📱 Enviando WhatsApp...");
        System.out.println("   From: " + fromWhatsApp);
        System.out.println("   To:   " + toNumber);
        System.out.println("   URL:  https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", fromWhatsApp);
        body.add("To", toNumber);
        body.add("Body", message.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Body:   " + response.getBody());
            message.setDeliveryStatus(DeliveryStatus.SENT);
        } catch (HttpClientErrorException e) {
            // Erro do Twilio com detalhes
            System.err.println("❌ HTTP Error: " + e.getStatusCode());
            System.err.println("❌ Response:   " + e.getResponseBodyAsString());
            message.setDeliveryStatus(DeliveryStatus.SEND_ERROR);
        } catch (Exception e) {
            System.err.println("❌ Erro geral: " + e.getMessage());
            message.setDeliveryStatus(DeliveryStatus.SEND_ERROR);
        }
    }
}