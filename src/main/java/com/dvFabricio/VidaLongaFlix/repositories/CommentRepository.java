package com.dvFabricio.VidaLongaFlix.repositories;


import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByVideoUuid(UUID videoUuid);

    List<Comment> findByUserUuid(UUID userUuid);

    boolean existsByTextAndUserUuidAndVideoUuid(String text, UUID userUuid, UUID videoUuid);
}
