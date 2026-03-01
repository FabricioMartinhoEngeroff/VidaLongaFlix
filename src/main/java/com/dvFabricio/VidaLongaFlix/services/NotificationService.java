package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.controllers.notifications.dto.NotificationItemDTO;
import com.dvFabricio.VidaLongaFlix.controllers.notifications.dto.NotificationsPageDTO;
import com.dvFabricio.VidaLongaFlix.controllers.notifications.dto.UnreadCountDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.notification.Notification;
import com.dvFabricio.VidaLongaFlix.domain.notification.NotificationType;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.NotificationRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NotificationsPageDTO getNotifications(User user, int page, int size) {
        Page<Notification> result = notificationRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));

        Instant lastReadAt = user.getNotificationsLastReadAt();
        List<NotificationItemDTO> items = result.getContent().stream()
                .map(n -> NotificationItemDTO.of(n, lastReadAt))
                .toList();

        return new NotificationsPageDTO(items, result.hasNext());
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getUnreadCount(User user) {
        Instant lastReadAt = user.getNotificationsLastReadAt();
        long count = lastReadAt == null
                ? notificationRepository.count()
                : notificationRepository.countByCreatedAtAfter(lastReadAt);
        return new UnreadCountDTO(count);
    }

    @Transactional
    public void markAllAsRead(User user) {
        user.setNotificationsLastReadAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void createForVideo(Video video) {
        Notification notification = new Notification();
        notification.setType(NotificationType.VIDEO);
        notification.setTitle(video.getTitle());
        notification.setContentId(video.getId());
        notificationRepository.save(notification);
    }

    @Transactional
    public void createForMenu(Menu menu) {
        Notification notification = new Notification();
        notification.setType(NotificationType.MENU);
        notification.setTitle(menu.getTitle());
        notification.setContentId(menu.getId());
        notificationRepository.save(notification);
    }
}
