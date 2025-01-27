package com.dvFabricio.VidaLongaFlix.repositories;


import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {


    List<Comment> findByVideo_Id(UUID videoId);


    List<Comment> findByUser_Id(UUID userId);


    boolean existsByTextAndUser_IdAndVideo_Id(String text, UUID userId, UUID videoId);


}
