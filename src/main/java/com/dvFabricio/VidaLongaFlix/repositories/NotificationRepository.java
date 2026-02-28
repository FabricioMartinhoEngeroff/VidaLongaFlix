package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByCreatedAtAfter(Instant lastReadAt);
}
