package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.endereco.Endereco;

public record UserProfileDTO(
        String name,
        String email,
        String cpf,
        String telefone,
        Endereco endereco
) {}
