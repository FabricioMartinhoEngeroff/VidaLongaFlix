package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.message.DeliveryStatus;
import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from}")
    private String fromNumber;

    @Async
    public void send(Message message) {
        RestTemplate restTemplate = new RestTemplate();

        // Twilio expects form-urlencoded, not JSON.
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", "whatsapp:" + fromNumber);
        body.add("To", "whatsapp:+55" + message.getDestination().replaceAll("[^0-9]", ""));
        body.add("Body", message.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);  // Twilio uses Basic Auth.
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        try {
            restTemplate.postForEntity(url, request, String.class);
            message.setDeliveryStatus(DeliveryStatus.SENT);
            System.out.println("WhatsApp enviado com sucesso para " + message.getDestination());
        } catch (Exception e) {
            message.setDeliveryStatus(DeliveryStatus.SEND_ERROR);
            throw new ResourceAccessException("Falha ao enviar WhatsApp via Twilio: " + e.getMessage(), e);
        }
    }
}
