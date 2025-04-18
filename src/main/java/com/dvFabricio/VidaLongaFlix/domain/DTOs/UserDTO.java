package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.endereco.Endereco;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        String email,
        List<String> roles,
        String cpf,
        String telefone,
        Endereco endereco
) {
    public UserDTO(User user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles() != null && !user.getRoles().isEmpty()
                        ? user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
                        : List.of(),
                user.getCpf(),
                user.getTelefone(),
                user.getEndereco()
        );
    }

    public String getRua() {
        return Optional.ofNullable(endereco).map(Endereco::getRua).orElse(null);
    }

}

