package com.dvFabricio.VidaLongaFlix.domain.auth;

import com.dvFabricio.VidaLongaFlix.domain.user.UserResponseDTO;

public record AuthResponseDTO(
        String token,
        UserResponseDTO user
) {}
