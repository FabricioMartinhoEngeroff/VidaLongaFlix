package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import com.dvFabricio.VidaLongaFlix.domain.message.Message;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WaitlistNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistNotificationService.class);
    private static final String PLATFORM_URL = "https://vidalongaflix.com";

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    public WaitlistNotificationService(EmailService emailService, WhatsAppService whatsAppService) {
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
    }

    public void notifyQueued(User user) {
        emailService.send(new EmailMessage(
                user.getEmail(),
                "VidaLongaFlix - Fila de espera",
                buildQueuedEmailBody(user)
        ));
        logger.info("Queue notification sent for user {} at position {}", user.getEmail(), user.getQueuePosition());
    }

    public void notifyActivated(User user) {
        emailService.send(new EmailMessage(
                user.getEmail(),
                "VidaLongaFlix - Conta ativada",
                buildActivatedEmailBody(user)
        ));
        logger.info("Activation notification sent for user {}", user.getEmail());
        whatsAppService.send(new Message(user.getPhone(), "account_activated_template"));
    }

    public void notifyRemoved(User user) {
        emailService.send(new EmailMessage(
                user.getEmail(),
                "VidaLongaFlix - Saida da fila de espera",
                buildRemovedEmailBody(user)
        ));
        logger.info("Removal notification sent for user {}", user.getEmail());
    }

    private String buildQueuedEmailBody(User user) {
        return "Ola, " + user.getName() + ". Seu cadastro foi adicionado a fila de espera do VidaLongaFlix. "
                + "Sua posicao atual e #" + user.getQueuePosition() + ". "
                + "Assim que novas vagas forem liberadas, voce sera avisado(a).";
    }

    private String buildActivatedEmailBody(User user) {
        return "Ola, " + user.getName() + ". Sua conta no VidaLongaFlix foi ativada. "
                + "Acesse " + PLATFORM_URL + " para fazer login.";
    }

    private String buildRemovedEmailBody(User user) {
        return "Ola, " + user.getName() + ". Seu cadastro foi removido da fila de espera do VidaLongaFlix. "
                + "Se quiser, voce pode realizar um novo cadastro quando houver interesse.";
    }
}
