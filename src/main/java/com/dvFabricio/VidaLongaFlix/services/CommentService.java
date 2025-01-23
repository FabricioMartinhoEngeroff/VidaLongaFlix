package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, VideoRepository videoRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentDTO createComment(CommentDTO commentDTO) {
        validateCommentFields(commentDTO);

        User user = userRepository.findById(commentDTO.user().id())
                .orElseThrow(() -> new ResourceNotFoundExceptions("User with UUID " + commentDTO.user().id() + " not found."));

        Video video = videoRepository.findByUuid(commentDTO.videoUuid())
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with UUID " + commentDTO.videoUuid() + " not found."));

        if (commentRepository.existsByTextAndUserUuidAndVideoUuid(commentDTO.text(), user.getId(), video.getUuid())) {
            throw new DatabaseException("Duplicate comment: same user, video, and text.");
        }

        try {
            Comment comment = new Comment();
            comment.setText(commentDTO.text());
            comment.setUser(user);
            comment.setVideo(video);
            comment.setDate(LocalDateTime.now());
            comment = commentRepository.save(comment);
            return new CommentDTO(comment);
        } catch (Exception e) {
            throw new DatabaseException("Error while creating comment: " + e.getMessage());
        }
    }

    public List<CommentDTO> getCommentsByVideo(UUID videoUuid) {
        List<Comment> comments = commentRepository.findByVideoUuid(videoUuid);
        if (comments.isEmpty()) {
            throw new ResourceNotFoundExceptions("No comments found for video with UUID " + videoUuid);
        }
        return comments.stream().map(CommentDTO::new).collect(Collectors.toList());
    }

    public List<CommentDTO> getCommentsByUser(UUID userUuid) {
        List<Comment> comments = commentRepository.findByUserUuid(userUuid);
        if (comments.isEmpty()) {
            throw new ResourceNotFoundExceptions("No comments found for user with UUID " + userUuid);
        }
        return comments.stream().map(CommentDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(UUID commentUuid) {
        Comment comment = commentRepository.findById(commentUuid)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Comment with UUID " + commentUuid + " not found."));
        try {
            commentRepository.delete(comment);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting comment with UUID " + commentUuid + ": " + e.getMessage());
        }
    }

    private void validateCommentFields(CommentDTO commentDTO) {
        if (commentDTO.text() == null || commentDTO.text().isBlank()) {
            throw new MissingRequiredFieldException("text", "The comment text is required.");
        }
    }
}

