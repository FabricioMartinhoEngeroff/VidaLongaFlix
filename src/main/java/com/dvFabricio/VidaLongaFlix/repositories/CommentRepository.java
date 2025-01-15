//package com.dvFabricio.VidaLongaFlix.repositories;
//
//import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.UUID;
//
//@Repository
//public interface CommentRepository extends JpaRepository<Comment, UUID> {
//    Page<Comment> findByVideo_Id(Long videoId, Pageable pageable);
//}