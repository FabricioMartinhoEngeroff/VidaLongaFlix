package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeService.class);
    private static final String PLATFORM_URL = "https://vidalongaflix.com";
    private static final String NUTRITIONIST_WHATSAPP = "(51) 97810-0460";
    private static final String NUTRITIONIST_EMAIL = "Amandafidelismuraro@gmail.com";

    private final EmailService emailService;

    public WelcomeService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendWelcomeMessage(String name, String email) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name cannot be blank");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("email cannot be blank");

        try {
            emailService.send(new EmailMessage(
                    email,
                    "Bem-vindo(a) ao VidaLongaFlix, " + name + "!",
                    buildBody(name)
            ));
            logger.info("Welcome email sent to {}", email);
        } catch (Exception e) {
            logger.error("Welcome email not sent to {}: {}", email, e.getMessage());
        }
    }

    private String buildBody(String name) {
        return "Ola, " + name + "!\n\n"
                + "Seja bem-vindo(a) ao VidaLongaFlix — a plataforma de saude, nutricao e bem-estar\n"
                + "criada pela nutricionista Amanda Fidelis Muraro.\n\n"
                + "Seu acesso esta ativo. Acesse agora e explore os conteudos exclusivos:\n"
                + PLATFORM_URL + "\n\n"
                + "---\n"
                + "Quer uma consulta personalizada com a Dra. Amanda?\n\n"
                + "WhatsApp: " + NUTRITIONIST_WHATSAPP + "\n"
                + "E-mail:   " + NUTRITIONIST_EMAIL + "\n\n"
                + "---\n"
                + "Qualquer duvida, responda este email.\n\n"
                + "Equipe VidaLongaFlix";
    }
}
