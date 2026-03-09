package com.dvFabricio.VidaLongaFlix.domain.auth;

public record QueueLoginErrorDTO(
        String error,
        String message,
        Integer queuePosition
) {
}
