package com.dvFabricio.VidaLongaFlix.domain.auth;


import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        String exMessage,
        String path,
        int status,
        LocalDateTime timestamp
) {}

