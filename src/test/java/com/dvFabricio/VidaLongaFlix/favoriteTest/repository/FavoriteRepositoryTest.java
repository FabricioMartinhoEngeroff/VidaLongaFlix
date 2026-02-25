package com.dvFabricio.VidaLongaFlix.favoriteTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.UserFavorite;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.repositories.FavoriteRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FavoriteRepositoryTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        user1 = new User("João", "joao@example.com", "Password1@", "(11) 99999-9999");
        user1.setTaxId("123.456.789-00");
        user2 = new User("Maria", "maria@example.com", "Password1@", "(11) 88888-8888");
        user2.setTaxId("987.654.321-00");
        userRepository.saveAndFlush(user1);
        userRepository.saveAndFlush(user2);

        em.persistAndFlush(UserFavorite.builder()
                .user(user1).itemId("video-1").itemType(FavoriteContentType.VIDEO).build());
        em.persistAndFlush(UserFavorite.builder()
                .user(user1).itemId("menu-1").itemType(FavoriteContentType.MENU).build());
        em.persistAndFlush(UserFavorite.builder()
                .user(user2).itemId("video-1").itemType(FavoriteContentType.VIDEO).build());
    }

    @Test
    void shouldFindAllByUserId() {
        List<UserFavorite> result = favoriteRepository.findByUser_Id(user1.getId());
        assertEquals(2, result.size());
    }

    @Test
    void shouldFindByUserIdAndItemType() {
        List<UserFavorite> videos = favoriteRepository
                .findByUser_IdAndItemType(user1.getId(), FavoriteContentType.VIDEO);
        assertEquals(1, videos.size());
        assertEquals("video-1", videos.get(0).getItemId());

        List<UserFavorite> menus = favoriteRepository
                .findByUser_IdAndItemType(user1.getId(), FavoriteContentType.MENU);
        assertEquals(1, menus.size());
        assertEquals("menu-1", menus.get(0).getItemId());
    }

    @Test
    void shouldFindByUserIdAndItemIdAndItemType() {
        Optional<UserFavorite> result = favoriteRepository
                .findByUser_IdAndItemIdAndItemType(
                        user1.getId(), "video-1", FavoriteContentType.VIDEO);
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<UserFavorite> result = favoriteRepository
                .findByUser_IdAndItemIdAndItemType(
                        user1.getId(), "video-999", FavoriteContentType.VIDEO);
        assertFalse(result.isPresent());
    }

    @Test
    void shouldCheckExists() {
        assertTrue(favoriteRepository.existsByUser_IdAndItemIdAndItemType(
                user1.getId(), "video-1", FavoriteContentType.VIDEO));
        assertFalse(favoriteRepository.existsByUser_IdAndItemIdAndItemType(
                user1.getId(), "video-999", FavoriteContentType.VIDEO));
    }

    @Test
    void shouldCountLikes() {
        assertEquals(2L, favoriteRepository
                .countByItemIdAndItemType("video-1", FavoriteContentType.VIDEO));
        assertEquals(1L, favoriteRepository
                .countByItemIdAndItemType("menu-1", FavoriteContentType.MENU));
    }

    @Test
    void shouldReturnZeroForUnknownItem() {
        assertEquals(0L, favoriteRepository
                .countByItemIdAndItemType("unknown", FavoriteContentType.VIDEO));
    }
}