package com.dvFabricio.VidaLongaFlix.controllers.notifications.dto;

import com.dvFabricio.VidaLongaFlix.domain.notification.Notification;
import com.dvFabricio.VidaLongaFlix.domain.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationItemDTO(
        UUID id,
        NotificationType type,
        String title,
        UUID contentId,
        Instant createdAt,
        boolean read
) {
    public static NotificationItemDTO of(Notification n, Instant lastReadAt) {
        boolean read = lastReadAt != null && !n.getCreatedAt().isAfter(lastReadAt);
        return new NotificationItemDTO(
                n.getId(), n.getType(), n.getTitle(), n.getContentId(), n.getCreatedAt(), read
        );
    }
}
