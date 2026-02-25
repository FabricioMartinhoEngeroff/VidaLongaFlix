package com.dvFabricio.VidaLongaFlix.domain.user;


public record UserSummaryDTO(String id, String name) {

    public UserSummaryDTO(User user) {
        this(user.getId().toString(), user.getName());
    }
}
