package com.dvFabricio.VidaLongaFlix.domain.user;

import com.dvFabricio.VidaLongaFlix.domain.address.Address;
import java.util.List;

public record UserResponseDTO(
        String id,
        String name,
        String email,
        String taxId,
        String phone,
        Address address,
        String photo,
        boolean profileComplete,
        List<String> roles  // <- adicionar
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
                user.isProfileComplete(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );
    }
}