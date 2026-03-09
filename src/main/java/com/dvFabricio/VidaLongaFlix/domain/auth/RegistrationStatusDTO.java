package com.dvFabricio.VidaLongaFlix.domain.auth;

public record RegistrationStatusDTO(
        boolean open,
        long activeUsers,
        int limit,
        long queueSize
) {
}
