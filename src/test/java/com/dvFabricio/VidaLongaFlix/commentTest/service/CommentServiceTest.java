package com.dvFabricio.VidaLongaFlix.commentTest.service;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.infra.exception.comment.CommentNotFoundException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import com.dvFabricio.VidaLongaFlix.services.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    private Comment comment;
    private Video video;
    private User user;
    private CommentDTO commentDTO;

    @BeforeEach
    void setup() {
        user = new User("username", "user@example.com", "password");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        video = Video.builder().title("Video Title").description("Video Description").url("http://example.com/video").views(100).watchTime(50.5).build();
        ReflectionTestUtils.setField(video, "id", UUID.randomUUID());

        comment = new Comment();
        comment.setText("Sample comment");
        comment.setUser(user);
        comment.setVideo(video);
        comment.setDate(LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());

        commentDTO = new CommentDTO(comment.getId(), comment.getText(), comment.getDate(), user.getId(), video.getId());
    }

    @Test
    void shouldCreateNewComment() {
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(videoRepository.findById(video.getId())).willReturn(Optional.of(video));
        given(commentRepository.existsByTextAndUser_IdAndVideo_Id(comment.getText(), user.getId(), video.getId())).willReturn(false);

        assertDoesNotThrow(() -> commentService.create(commentDTO));

        then(userRepository).should().findById(user.getId());
        then(videoRepository).should().findById(video.getId());
        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    void shouldThrowExceptionWhenDuplicateComment() {

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(videoRepository.findById(video.getId())).willReturn(Optional.of(video));
        given(commentRepository.existsByTextAndUser_IdAndVideo_Id(comment.getText(), user.getId(), video.getId())).willReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> commentService.create(commentDTO));

        assertEquals("Duplicate comment: same user, video, and text.", exception.getMessage());
        assertEquals("text", exception.getField());

        then(commentRepository).should().existsByTextAndUser_IdAndVideo_Id(comment.getText(), user.getId(), video.getId());
    }


    @Test
    void shouldReturnTotalCommentsOnPlatform() {
        given(commentRepository.count()).willReturn(100L);

        long totalComments = commentService.getTotalCommentsOnPlatform();

        assertEquals(100L, totalComments);
        then(commentRepository).should().count();
    }

    @Test
    void shouldReturnTotalCommentsByVideo() {
        given(commentRepository.findAll()).willReturn(List.of(comment));

        Map<UUID, Long> result = commentService.getTotalCommentsByVideo();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(video.getId()));
        then(commentRepository).should().findAll();
    }

    @Test
    void shouldReturnCommentsByUser() {
        given(commentRepository.findByUser_Id(user.getId())).willReturn(List.of(comment));

        List<CommentDTO> comments = commentService.getCommentsByUser(user.getId());

        assertEquals(1, comments.size());
        assertEquals(comment.getText(), comments.get(0).text());
        then(commentRepository).should().findByUser_Id(user.getId());
    }

    @Test
    void shouldThrowExceptionWhenNoCommentsByUser() {
        given(commentRepository.findByUser_Id(user.getId())).willReturn(List.of());

        ResourceNotFoundExceptions exception = assertThrows(ResourceNotFoundExceptions.class, () -> commentService.getCommentsByUser(user.getId()));

        assertEquals("No comments found for user with ID " + user.getId(), exception.getMessage());
        then(commentRepository).should().findByUser_Id(user.getId());
    }

    @Test
    void shouldReturnCommentsByVideo() {
        given(commentRepository.findByVideo_Id(video.getId())).willReturn(List.of(comment));

        List<CommentDTO> comments = commentService.getCommentsByVideo(video.getId());

        assertEquals(1, comments.size());
        assertEquals(comment.getText(), comments.get(0).text());
        then(commentRepository).should().findByVideo_Id(video.getId());
    }

    @Test
    void shouldReturnUserNamesFromCommentsByVideo() {
        given(commentRepository.findByVideo_Id(video.getId())).willReturn(List.of(comment));

        List<String> userNames = commentService.getUserNamesFromCommentsByVideo(video.getId());

        assertEquals(1, userNames.size());
        assertEquals(user.getLogin(), userNames.get(0));
        then(commentRepository).should().findByVideo_Id(video.getId());
    }

    @Test
    void shouldReturnCommentCountByVideo() {
        given(commentRepository.findByVideo_Id(video.getId())).willReturn(List.of(comment));

        int count = commentService.getCommentCountByVideo(video.getId());

        assertEquals(1, count);
        then(commentRepository).should().findByVideo_Id(video.getId());
    }

    @Test
    void shouldDeleteComment() {
        UUID commentId = comment.getId();
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        assertDoesNotThrow(() -> commentService.delete(commentId));

        then(commentRepository).should().findById(commentId);
        then(commentRepository).should().delete(comment);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentComment() {
        UUID commentId = UUID.randomUUID();
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        CommentNotFoundException exception = assertThrows(CommentNotFoundException.class, () -> commentService.delete(commentId));

        assertEquals("Comment with ID " + commentId + " not found.", exception.getMessage());
        then(commentRepository).should().findById(commentId);
    }
}
