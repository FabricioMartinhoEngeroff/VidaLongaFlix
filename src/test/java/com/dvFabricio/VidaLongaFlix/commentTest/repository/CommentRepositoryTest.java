package com.dvFabricio.VidaLongaFlix.commentTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.video.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Category category;
    private Video video;
    private Comment comment1;
    private Comment comment2;

    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setup() {
        commentRepository.deleteAll();
        videoRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        category = new Category();
        category.setName("Education");
        categoryRepository.saveAndFlush(category);

        User user1 = new User("user1", "user1@example.com", "password");
        User user2 = new User("user2", "user2@example.com", "password");

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        userId1 = user1.getId();
        userId2 = user2.getId();

        video = Video.builder().title("Sample Video").description("A test video").url("http://example.com/video").category(category).views(300).watchTime(50.0).build();

        videoRepository.saveAndFlush(video);

        comment1 = Comment.builder().text("Great video!").date(LocalDateTime.now()).user(user1).video(video).build();

        comment2 = Comment.builder().text("Very informative!").date(LocalDateTime.now()).user(user2).video(video).build();

        entityManager.persistAndFlush(comment1);
        entityManager.persistAndFlush(comment2);
    }

    @Test
    void shouldFindCommentsByVideoId() {
        List<Comment> comments = commentRepository.findByVideo_Id(video.getId());

        assertAll(() -> assertEquals(2, comments.size(), "The video should have 2 comments"), () -> assertTrue(comments.contains(comment1), "Comment 1 should be present"), () -> assertTrue(comments.contains(comment2), "Comment 2 should be present"));
    }

    @Test
    void shouldFindCommentsByUserId() {
        List<Comment> user1Comments = commentRepository.findByUser_Id(userId1);
        List<Comment> user2Comments = commentRepository.findByUser_Id(userId2);

        assertAll(() -> assertEquals(1, user1Comments.size(), "User 1 should have 1 comment"), () -> assertEquals("Great video!", user1Comments.get(0).getText(), "The comment should be 'Great video!'"));

        assertAll(() -> assertEquals(1, user2Comments.size(), "User 2 should have 1 comment"), () -> assertEquals("Very informative!", user2Comments.get(0).getText(), "The comment should be 'Very informative!'"));
    }

    @Test
    void shouldCheckIfCommentExistsByTextUserAndVideo() {
        boolean exists = commentRepository.existsByTextAndUser_IdAndVideo_Id("Great video!", userId1, video.getId());

        assertTrue(exists, "The comment with text 'Great video!' should exist for user 1 on the video.");
    }

    @Test
    void shouldReturnFalseIfCommentDoesNotExist() {
        boolean exists = commentRepository.existsByTextAndUser_IdAndVideo_Id("Non-existent comment", userId1, video.getId());

        assertFalse(exists, "A non-existent comment should not be found.");
    }
}


