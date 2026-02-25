package com.dvFabricio.VidaLongaFlix.domain.user;

import com.dvFabricio.VidaLongaFlix.domain.address.Address;

public record UserProfileDTO(
        String name,
        String email,
        String taxId,
        String phone,
        Address address
) {}
