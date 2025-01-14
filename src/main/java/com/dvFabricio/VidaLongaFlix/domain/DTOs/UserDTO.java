package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.user.User;


import java.util.UUID;


public record UserDTO(
        UUID id,
        String login,
        String email,
        String role
) {
    public UserDTO(User user) {
        this(
                user.getId(),
                user.getLogin(),
                user.getEmail(),
                user.getRole().getRole()
        );
    }
}