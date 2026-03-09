package com.dvFabricio.VidaLongaFlix.domain.waitlist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MaxUsersConfigRequestDTO(
        @NotNull(message = "maxActiveUsers is required")
        @Min(value = 1, message = "maxActiveUsers must be at least 1")
        Integer maxActiveUsers
) {
}
