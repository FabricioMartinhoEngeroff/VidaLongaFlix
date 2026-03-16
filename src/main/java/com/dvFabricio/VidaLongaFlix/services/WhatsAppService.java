package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.DeliveryStatus;
import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.api-version:v22.0}")
    private String apiVersion = "v22.0";

    @Value("${whatsapp.template.name:welcome_template}")
    private String templateName = "welcome_template";

    @Value("${whatsapp.template.language:pt_BR}")
    private String templateLanguage = "pt_BR";

    @Value("${whatsapp.enabled:false}")
    private boolean enabled = false;

    public void send(Message message) {
        if (!enabled) {
            System.out.println("[DEV] WhatsApp simulado para: " + message.getDestination());
            message.setDeliveryStatus(DeliveryStatus.SENT);
            return;
        }

        // Remove formatação e garante código do país 55 (Brasil)
        String digits = message.getDestination().replaceAll("[^0-9]", "");
        String toNumber = digits.startsWith("55") ? digits : "55" + digits;

        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";

        System.out.println("📱 Enviando WhatsApp Business...");
        System.out.println("   URL: " + url);
        System.out.println("   To:  " + toNumber);

        Map<String, Object> payload = buildTemplatePayload(toNumber, message.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ Status: " + response.getStatusCode());
            System.out.println("✅ Body:   " + response.getBody());
            message.setDeliveryStatus(DeliveryStatus.SENT);
        } catch (HttpClientErrorException e) {
            System.err.println("❌ HTTP Error: " + e.getStatusCode());
            System.err.println("❌ Response:   " + e.getResponseBodyAsString());
            message.setDeliveryStatus(DeliveryStatus.SEND_ERROR);
        } catch (Exception e) {
            System.err.println("❌ Erro geral: " + e.getMessage());
            message.setDeliveryStatus(DeliveryStatus.SEND_ERROR);
        }
    }

    private Map<String, Object> buildTemplatePayload(String toNumber, String messageTemplateName) {
        Map<String, Object> language = new LinkedHashMap<>();
        language.put("code", templateLanguage);

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", messageTemplateName != null && !messageTemplateName.isBlank() ? messageTemplateName : templateName);
        template.put("language", language);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toNumber);
        payload.put("type", "template");
        payload.put("template", template);

        return payload;
    }
}
