package com.dvFabricio.VidaLongaFlix.domain.DTOs;


import com.dvFabricio.VidaLongaFlix.domain.user.User;

public record UserSummaryDTO(String id, String name) {

    public UserSummaryDTO(User user) {
        this(user.getId().toString(), user.getName());
    }
}
