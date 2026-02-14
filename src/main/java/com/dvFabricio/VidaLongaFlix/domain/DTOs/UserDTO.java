package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.address.Address;
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
        String taxId,
        String phone,
        Address address
) {

    public UserDTO(User user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList(),

                user.getTaxId(),
                user.getPhone(),
                user.getAddress()
        );
    }

    public String getStreet() {
        return Optional.ofNullable(address)
                .map(Address::getStreet)
                .orElse(null); // Avoids NullPointerException when address is null
    }
}
