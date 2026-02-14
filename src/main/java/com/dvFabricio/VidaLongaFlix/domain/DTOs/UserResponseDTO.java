package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.address.Address;
import com.dvFabricio.VidaLongaFlix.domain.user.User;

public record UserResponseDTO(
        String id,
        String name,
        String email,
        String taxId,
        String phone,
        Address address,
        String photo,
        boolean profileComplete
) {
    public UserResponseDTO(User user) {
        this(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getTaxId(),
                user.getPhone(),
                user.getAddress(),
                user.getPhoto(),
                user.isProfileComplete()
        );
    }
}
