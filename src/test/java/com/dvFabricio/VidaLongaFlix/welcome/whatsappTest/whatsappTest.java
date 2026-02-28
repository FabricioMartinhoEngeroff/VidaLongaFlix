package com.dvFabricio.VidaLongaFlix.welcome.whatsappTest;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.services.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @InjectMocks
    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        // Injeta valores fake — não chama a API do Meta de verdade
        ReflectionTestUtils.setField(whatsAppService, "phoneNumberId", "fake-phone-id");
        ReflectionTestUtils.setField(whatsAppService, "accessToken", "fake-token");
        ReflectionTestUtils.setField(whatsAppService, "apiVersion", "v22.0");
        ReflectionTestUtils.setField(whatsAppService, "templateName", "hello_world");
        ReflectionTestUtils.setField(whatsAppService, "templateLanguage", "en_US");
        ReflectionTestUtils.setField(whatsAppService, "enabled", false); // <- modo dev
    }

    @Test
    void shouldNotSendWhenDisabled() {
        // Quando enabled=false não chama Twilio — só loga
        Message message = new Message("51999999999", "Olá teste!");

        assertDoesNotThrow(() -> whatsAppService.send(message));
    }
}