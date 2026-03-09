package com.dvFabricio.VidaLongaFlix.domain.waitlist;

import com.dvFabricio.VidaLongaFlix.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record WaitlistEntryDTO(
        UUID id,
        String name,
        String email,
        String phone,
        Integer position,
        LocalDateTime createdAt
) {
    public WaitlistEntryDTO(User user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getQueuePosition(),
                user.getCreatedAt()
        );
    }
}
