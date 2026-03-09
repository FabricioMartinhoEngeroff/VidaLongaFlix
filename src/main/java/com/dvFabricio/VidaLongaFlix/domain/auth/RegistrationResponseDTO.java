package com.dvFabricio.VidaLongaFlix.domain.auth;

import com.dvFabricio.VidaLongaFlix.domain.user.UserResponseDTO;

public record RegistrationResponseDTO(
        String token,
        UserResponseDTO user,
        boolean queued,
        Integer queuePosition,
        String message
) {
}
