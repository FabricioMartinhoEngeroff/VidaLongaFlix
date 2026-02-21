package com.dvFabricio.VidaLongaFlix.commentTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private CommentRepository commentRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private Video video;
    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        Category category = new Category("Education", CategoryType.VIDEO);
        categoryRepository.saveAndFlush(category);

        user1 = new User("user1", "user1@example.com", "password", "11999999991");
        user2 = new User("user2", "user2@example.com", "password", "11999999992");
        em.persistAndFlush(user1);
        em.persistAndFlush(user2);

        video = Video.builder()
                .title("Video").description("Desc")
                .url("http://example.com")
                .category(category).views(0).watchTime(0).build();
        videoRepository.saveAndFlush(video);

        em.persistAndFlush(Comment.builder()
                .text("Ótimo!").date(LocalDateTime.now())
                .user(user1).video(video).build());
        em.persistAndFlush(Comment.builder()
                .text("Muito bom!").date(LocalDateTime.now())
                .user(user2).video(video).build());
    }

    @Test
    void shouldFindCommentsByVideo() {
        List<Comment> comments = commentRepository.findByVideo_Id(video.getId());
        assertEquals(2, comments.size());
    }

    @Test
    void shouldFindCommentsByUser() {
        List<Comment> comments = commentRepository.findByUser_Id(user1.getId());
        assertEquals(1, comments.size());
        assertEquals("Ótimo!", comments.get(0).getText());
    }

    @Test
    void shouldCheckDuplicate() {
        assertTrue(commentRepository.existsByTextAndUser_IdAndVideo_Id(
                "Ótimo!", user1.getId(), video.getId()));
        assertFalse(commentRepository.existsByTextAndUser_IdAndVideo_Id(
                "Ótimo!", user2.getId(), video.getId()));
    }
}