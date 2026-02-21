package com.dvFabricio.VidaLongaFlix.commentTest.service;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CreateCommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks private CommentService commentService;
    @Mock private CommentRepository commentRepository;
    @Mock private VideoRepository videoRepository;
    @Mock private UserRepository userRepository;

    private User user;
    private Video video;
    private UUID userId;
    private UUID videoId;
    private CreateCommentDTO dto;

    @BeforeEach
    void setup() {
        user = new User("user1", "user1@example.com", "password", "11999999999");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        userId = user.getId();

        video = Video.builder()
                .title("Video").description("Desc")
                .url("http://example.com").views(0).watchTime(0).build();
        ReflectionTestUtils.setField(video, "id", UUID.randomUUID());
        videoId = video.getId();

        dto = new CreateCommentDTO("Ótimo vídeo!", videoId);
    }

    @Test
    void shouldCreateComment() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));
        given(commentRepository.existsByTextAndUser_IdAndVideo_Id(
                dto.text(), userId, videoId)).willReturn(false);

        assertDoesNotThrow(() -> commentService.create(dto, userId));
        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    void shouldThrowOnDuplicate() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));
        given(commentRepository.existsByTextAndUser_IdAndVideo_Id(
                dto.text(), userId, videoId)).willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> commentService.create(dto, userId));
        then(commentRepository).should(never()).save(any());
    }

    @Test
    void shouldReturnCommentsByVideo() {
        Comment comment = Comment.builder()
                .text("Ótimo!").user(user).video(video).build();
        given(commentRepository.findByVideo_Id(videoId))
                .willReturn(List.of(comment));

        var result = commentService.getCommentsByVideo(videoId);
        assertEquals(1, result.size());
    }

    @Test
    void shouldDeleteComment() {
        Comment comment = Comment.builder()
                .text("Ótimo!").user(user).video(video).build();
        UUID commentId = UUID.randomUUID();
        ReflectionTestUtils.setField(comment, "id", commentId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        assertDoesNotThrow(() -> commentService.delete(commentId));
        then(commentRepository).should().delete(comment);
    }
}