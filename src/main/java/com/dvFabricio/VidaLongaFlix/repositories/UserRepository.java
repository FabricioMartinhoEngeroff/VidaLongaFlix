package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByTaxId(String taxId);

    long countByStatus(UserStatus status);

    List<User> findByStatusOrderByQueuePositionAsc(UserStatus status);

    Optional<User> findFirstByStatusOrderByQueuePositionAsc(UserStatus status);

    @Query("select coalesce(max(u.queuePosition), 0) from User u where u.status = :status")
    Optional<Integer> findMaxQueuePosition(@Param("status") UserStatus status);
}
