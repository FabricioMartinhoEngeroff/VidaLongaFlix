//package com.dvFabricio.VidaLongaFlix.commentTest.domain;
//
//
//import com.dvFabricio.VidaLongaFlix.domain.video.Comment;
//import com.dvFabricio.VidaLongaFlix.domain.user.User;
//import com.dvFabricio.VidaLongaFlix.domain.video.Video;
//import jakarta.validation.Validation;
//import jakarta.validation.Validator;
//import jakarta.validation.ValidatorFactory;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.time.LocalDateTime;
//import java.util.Set;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//
//class CommentTest {
//
//    private User user;
//    private Video video;
//    private Comment comment1;
//    private Comment comment3;
//    private Validator validator;
//
//    @BeforeEach
//    void setup() {
//        // Initialize validator
//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//        validator = factory.getValidator();
//
//        // Mock User
//        user = new User("user1", "user1@example.com", "password");
//
//        // Mock Video
//        video = Video.builder().title("Sample Video").description("Test video").url("http://example.com/video").category(null).views(100).watchTime(10.0).build();
//
//        // Create comments with explicit UUIDs to avoid equality issues
//        comment1 = new Comment(UUID.randomUUID(), "Great video!", LocalDateTime.now(), user, video);
//        comment3 = new Comment(UUID.randomUUID(), "Different comment", LocalDateTime.now(), user, video);
//    }
//
//    @Test
//    void shouldCreateCommentSuccessfully() {
//        assertNotNull(comment1, "Comment should not be null");
//        assertNotNull(comment1.getId(), "ID should be auto-generated");
//        assertNotNull(comment1.getText(), "Comment text should not be null");
//        assertNotNull(comment1.getDate(), "Comment date should not be null");
//        assertNotNull(comment1.getUser(), "Comment user should not be null");
//        assertNotNull(comment1.getVideo(), "Comment video should not be null");
//    }
//
//    @Test
//    void shouldValidateCommentText() {
//        Comment invalidComment = new Comment(null, "", LocalDateTime.now(), user, video);
//
//        Set violations = validator.validate(invalidComment);
//        assertFalse(violations.isEmpty(), "Validation should fail for an empty text comment");
//    }
//
//    @Test
//    void shouldVerifyCommentEquality() {
//        assertNotEquals(comment1, comment3, "Different comments should not be equal");
//        assertEquals(comment1, comment1, "Same comment instance should be equal");
//    }
//
//    @Test
//    void shouldSetAndGetPropertiesCorrectly() {
//        comment1.setText("Updated Comment");
//        assertEquals("Updated Comment", comment1.getText(), "Comment text should be updated correctly");
//
//        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
//        comment1.setDate(newDate);
//        assertEquals(newDate, comment1.getDate(), "Comment date should be updated correctly");
//
//        User newUser = new User("user2", "user2@example.com", "password");
//        comment1.setUser(newUser);
//        assertEquals(newUser, comment1.getUser(), "Comment user should be updated correctly");
//
//        Video newVideo = Video.builder().title("New Video").description("New Description").url("http://example.com/new").category(null).views(200).watchTime(20.0).build();
//
//        comment1.setVideo(newVideo);
//        assertEquals(newVideo, comment1.getVideo(), "Comment video should be updated correctly");
//    }
//}
