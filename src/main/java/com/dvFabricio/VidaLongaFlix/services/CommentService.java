package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.infra.exception.comment.CommentNotFoundException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    public void create(CommentDTO commentDTO) {
        validateCommentFields(commentDTO);

        User user = findUserById(commentDTO.userId());
        Video video = findVideoById(commentDTO.videoId());

        if (commentRepository.existsByTextAndUser_IdAndVideo_Id(commentDTO.text(), user.getId(), video.getId())) {
            throw new DuplicateResourceException("text", "Duplicate comment: same user, video, and text.");
        }

        Comment comment = new Comment();
        comment.setText(commentDTO.text());
        comment.setUser(user);
        comment.setVideo(video);
        comment.setDate(LocalDateTime.now());

        saveComment(comment);
    }


    public long getTotalCommentsOnPlatform() {
        try {
            return commentRepository.count();
        } catch (Exception e) {
            throw new DatabaseException("Error retrieving total comments on the platform: " + e.getMessage());
        }
    }

    public Map<UUID, Long> getTotalCommentsByVideo() {
        try {
            return commentRepository.findAll().stream().collect(Collectors.groupingBy(comment -> comment.getVideo().getId(), Collectors.counting()));
        } catch (Exception e) {
            throw new DatabaseException("Error retrieving comment count by video: " + e.getMessage());
        }
    }

    public List<CommentDTO> getCommentsByUser(UUID userId) {
        List<Comment> comments = commentRepository.findByUser_Id(userId);
        if (comments.isEmpty()) {
            throw new ResourceNotFoundExceptions("No comments found for user with ID " + userId);
        }
        return comments.stream().map(CommentDTO::new).toList();
    }

    public List<CommentDTO> getCommentsByVideo(UUID videoId) {

        List<Comment> comments = commentRepository.findByVideo_Id(videoId);

        if (comments.isEmpty()) {
            throw new ResourceNotFoundExceptions("No comments found for video with ID " + videoId);
        }

        return comments.stream().map(CommentDTO::new).toList();
    }

    public int getCommentCountByVideo(UUID videoId) {
        return commentRepository.findByVideo_Id(videoId).size();
    }

    public List<String> getUserNamesFromCommentsByVideo(UUID videoId) {
        List<Comment> comments = commentRepository.findByVideo_Id(videoId);
        return comments.stream().map(comment -> comment.getUser().getLogin()).toList();
    }

    @Transactional
    public void delete(UUID commentId) {
        Comment comment = findCommentById(commentId);
        try {
            commentRepository.delete(comment);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting comment with ID " + commentId + ": " + e.getMessage());
        }
    }

    private void saveComment(Comment comment) {
        try {
            commentRepository.save(comment);
        } catch (Exception e) {
            throw new DatabaseException("Error while saving comment: " + e.getMessage());
        }
    }

    private Comment findCommentById(UUID commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException("Comment with ID " + commentId + " not found."));
    }

    private Video findVideoById(UUID videoId) {
        return videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundExceptions("Video with ID " + videoId + " not found."));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundExceptions("User with ID " + userId + " not found."));
    }

    private void validateCommentFields(CommentDTO commentDTO) {
        if (isBlank(commentDTO.text())) {
            throw new MissingRequiredFieldException("text", "The comment text is required.");
        }
    }

    private boolean isBlank(String field) {
        return field == null || field.isBlank();
    }
}



