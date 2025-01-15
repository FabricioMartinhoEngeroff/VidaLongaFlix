package com.dvFabricio.VidaLongaFlix.domain.DTOs;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequestDTO(
        @NotBlank(message = "Login não pode estar vazio")
        String login,

        @NotBlank(message = "Senha não pode estar vazia")
        String password,

        @NotBlank(message = "Email não pode estar vazio")
        @Email(message = "Email deve ser válido")
        String email
) {
}
