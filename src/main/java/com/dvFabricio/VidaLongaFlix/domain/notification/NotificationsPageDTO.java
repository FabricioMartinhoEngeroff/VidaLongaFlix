package com.dvFabricio.VidaLongaFlix.domain.notification;

import java.util.List;

public record NotificationsPageDTO(
        List<NotificationItemDTO> items,
        boolean hasMore
) {}
