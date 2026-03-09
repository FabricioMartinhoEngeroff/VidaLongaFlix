package com.dvFabricio.VidaLongaFlix.domain.waitlist;

import java.util.List;

public record WaitlistResponseDTO(
        int limit,
        long activeUsers,
        List<WaitlistEntryDTO> queue
) {
}
