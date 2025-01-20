package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;


import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String login,
        String email,
        List<String> roles,
        String password
) {
    public UserDTO(User user) {
        this(
                user.getId(),
                user.getLogin(),
                user.getEmail(),
                user.getRoles() != null && !user.getRoles().isEmpty()
                        ? user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
                        : List.of(),
                user.getPassword()
        );
    }
}

