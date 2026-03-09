package com.dvFabricio.VidaLongaFlix.welcome.whatsappTest;

import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.domain.message.DeliveryStatus;
import com.dvFabricio.VidaLongaFlix.services.WhatsAppService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @InjectMocks
    private WhatsAppService whatsAppService;

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        // Injeta valores fake — não chama a API do Meta de verdade
        ReflectionTestUtils.setField(whatsAppService, "phoneNumberId", "fake-phone-id");
        ReflectionTestUtils.setField(whatsAppService, "accessToken", "fake-token");
        ReflectionTestUtils.setField(whatsAppService, "apiVersion", "v22.0");
        ReflectionTestUtils.setField(whatsAppService, "templateName", "hello_world");
        ReflectionTestUtils.setField(whatsAppService, "templateLanguage", "en_US");
        ReflectionTestUtils.setField(whatsAppService, "enabled", false); // <- modo dev

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(whatsAppService, "enabled", false);
        System.setOut(originalOut);
    }

    @Test
    void shouldNotSendWhenDisabled() {
        // Quando enabled=false não chama Twilio — só loga
        Message message = new Message("51999999999", "Olá teste!");

        assertDoesNotThrow(() -> whatsAppService.send(message));
        assertEquals(DeliveryStatus.SENT, message.getDeliveryStatus());
    }

    @Test
    void shouldNormalizeMaskedMobilePhoneBeforeCallingMetaApi() {
        ReflectionTestUtils.setField(whatsAppService, "enabled", true);
        Message message = new Message("(11) 98765-4321", "welcome_template");

        whatsAppService.send(message);

        assertTrue(outContent.toString().contains("To:  5511987654321"));
        assertEquals(DeliveryStatus.SEND_ERROR, message.getDeliveryStatus());
    }

    @Test
    void shouldKeepCountryCodeWhenPhoneAlreadyStartsWith55() {
        ReflectionTestUtils.setField(whatsAppService, "enabled", true);
        Message message = new Message("5511987654321", "welcome_template");

        whatsAppService.send(message);

        assertTrue(outContent.toString().contains("To:  5511987654321"));
        assertEquals(DeliveryStatus.SEND_ERROR, message.getDeliveryStatus());
    }

    @Test
    void shouldMarkMessageAsErrorWhenMetaApiCallFails() {
        ReflectionTestUtils.setField(whatsAppService, "enabled", true);
        Message message = new Message("(11) 98765-4321", "welcome_template");

        assertDoesNotThrow(() -> whatsAppService.send(message));
        assertEquals(DeliveryStatus.SEND_ERROR, message.getDeliveryStatus());
    }
}
