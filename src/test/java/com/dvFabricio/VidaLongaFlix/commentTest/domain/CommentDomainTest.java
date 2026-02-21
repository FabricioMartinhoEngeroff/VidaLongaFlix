package com.dvFabricio.VidaLongaFlix.commentTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommentDomainTest {

    private Validator validator;
    private User user;
    private Video video;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        user = new User("user1", "user1@example.com", "password", "11999999999");
        video = Video.builder()
                .title("Video").description("Desc")
                .url("http://example.com").views(0).watchTime(0).build();
    }

    @Test
    void shouldCreateCommentSuccessfully() {
        Comment comment = Comment.builder()
                .text("Ótimo vídeo!")
                .date(LocalDateTime.now())
                .user(user).video(video).build();

        assertNotNull(comment.getText());
        assertNotNull(comment.getDate());
        assertNotNull(comment.getUser());
        assertNotNull(comment.getVideo());
    }

    @Test
    void shouldFailWhenTextIsBlank() {
        Comment comment = new Comment(null, "", LocalDateTime.now(), user, video);
        Set violations = validator.validate(comment);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldUpdateProperties() {
        Comment comment = Comment.builder()
                .text("original").date(LocalDateTime.now())
                .user(user).video(video).build();

        comment.setText("atualizado");
        assertEquals("atualizado", comment.getText());
    }
}