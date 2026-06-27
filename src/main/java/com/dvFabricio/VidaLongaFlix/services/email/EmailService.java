package com.dvFabricio.VidaLongaFlix.services.email;

import com.dvFabricio.VidaLongaFlix.domain.email.EmailMessage;

public interface EmailService {

    void send(EmailMessage message);
}
