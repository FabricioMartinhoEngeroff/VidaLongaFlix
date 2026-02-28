package com.dvFabricio.VidaLongaFlix.controllers.notifications.dto;

import java.util.List;

public record NotificationsPageDTO(
        List<NotificationItemDTO> items,
        boolean hasMore
) {}
