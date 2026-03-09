package com.dvFabricio.VidaLongaFlix.domain.email;

public record EmailMessage(
        String to,
        String subject,
        String body
) {
}
