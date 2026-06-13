package com.dvFabricio.VidaLongaFlix.domain.passwordreset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record PasswordResetRequestDTO(

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email

) {}

