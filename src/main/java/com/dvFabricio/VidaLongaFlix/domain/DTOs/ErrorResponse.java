package com.dvFabricio.VidaLongaFlix.domain.DTOs;


import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        String exMessage,
        String path,
        int status,
        LocalDateTime timestamp
) {}

