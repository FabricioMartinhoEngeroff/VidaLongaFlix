package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "Email não pode estar vazio")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Senha não pode estar vazia")
        @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
        String password
) {}