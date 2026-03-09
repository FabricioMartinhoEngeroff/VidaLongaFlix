package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void send(EmailMessage message) {
        logger.info(
                "Email dispatch requested to={} subject=\"{}\" body=\"{}\"",
                message.to(),
                message.subject(),
                message.body()
        );
    }
}
