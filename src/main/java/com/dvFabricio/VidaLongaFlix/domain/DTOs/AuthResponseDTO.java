package com.dvFabricio.VidaLongaFlix.domain.DTOs;

public record AuthResponseDTO(
        String token,
        UserResponseDTO user
) {}
