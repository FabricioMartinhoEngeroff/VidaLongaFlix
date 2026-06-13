package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private static final String FRONTEND_URL = "https://vidalongaflix.com";

    private final EmailService emailService;

    public PasswordResetEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendResetEmail(String name, String email, String token) {
        String subject = "VidaLongaFlix — Solicitação de redefinição de senha";

        String link = FRONTEND_URL + "/password-change?token=" + token;

        String body = "Olá, " + name + ".\n\n"
                + "Recebemos uma solicitação para redefinir a senha da sua conta no VidaLongaFlix.\n\n"
                + "Clique no link abaixo para criar uma nova senha (válido por 30 minutos):\n\n"
                + link + "\n\n"
                + "Se você não solicitou a redefinição, pode ignorar este email com segurança.\n"
                + "Sua senha atual permanecerá sem alterações.\n\n"
                + "---\nEquipe VidaLongaFlix";

        emailService.send(new EmailMessage(email, subject, body));
    }

    public void sendChangeConfirmation(String name, String email) {
        try {
            String subject = "VidaLongaFlix — Sua senha foi alterada";

            String body = "Olá, " + name + ".\n\n"
                    + "Sua senha do VidaLongaFlix foi alterada com sucesso.\n\n"
                    + "Se você não realizou essa alteração, entre em contato conosco imediatamente "
                    + "respondendo este email.\n\n"
                    + "---\nEquipe VidaLongaFlix";

            emailService.send(new EmailMessage(email, subject, body));
        } catch (Exception e) {
            logger.error("Falha ao enviar email de confirmação de troca de senha para {}: {}", email, e.getMessage());
        }
    }
}
