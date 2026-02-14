package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank(message = "Nome não pode estar vazio")
        String name,

        @NotBlank(message = "Email não pode estar vazio")
        @Email(message = "Email deve ser válido")
        String email,

        @NotBlank(message = "Senha não pode estar vazia")
        @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
                message = "A senha deve conter pelo menos 8 caracteres, incluindo maiúscula, minúscula, número e caractere especial"
        )
        String password,

        @NotBlank(message = "Telefone não pode estar vazio")
        @Pattern(regexp = "\\(\\d{2}\\) \\d{4,5}-\\d{4}", message = "Telefone deve estar no formato (XX) XXXXX-XXXX")
        String phone
) {}
