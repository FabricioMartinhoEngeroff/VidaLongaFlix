package com.dvFabricio.VidaLongaFlix.domain.waitlist;

public record MaxUsersConfigResponseDTO(
        int maxActiveUsers,
        long activeUsers,
        int promotedFromQueue
) {
}
