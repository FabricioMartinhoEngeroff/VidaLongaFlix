package com.dvFabricio.VidaLongaFlix.favoriteTest.service;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteDTO;
import com.dvFabricio.VidaLongaFlix.domain.favorite.UserFavorite;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.FavoriteRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.services.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @InjectMocks private FavoriteService favoriteService;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private UserRepository userRepository;

    private User user;
    private UUID userId;

    @BeforeEach
    void setup() {
        user = new User("João", "joao@example.com", "Password1@", "(11) 99999-9999");
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    void shouldAddFavoriteWhenNotExists() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(favoriteRepository.findByUser_IdAndItemIdAndItemType(
                userId, "video-1", FavoriteContentType.VIDEO))
                .willReturn(Optional.empty());

        boolean result = favoriteService.toggle(userId, "video-1", FavoriteContentType.VIDEO);

        assertTrue(result);
        then(favoriteRepository).should().save(any(UserFavorite.class));
    }

    @Test
    void shouldRemoveFavoriteWhenAlreadyExists() {
        UserFavorite existing = UserFavorite.builder()
                .user(user).itemId("video-1")
                .itemType(FavoriteContentType.VIDEO).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(favoriteRepository.findByUser_IdAndItemIdAndItemType(
                userId, "video-1", FavoriteContentType.VIDEO))
                .willReturn(Optional.of(existing));

        boolean result = favoriteService.toggle(userId, "video-1", FavoriteContentType.VIDEO);

        assertFalse(result);
        then(favoriteRepository).should().delete(existing);
        then(favoriteRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnToggle() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> favoriteService.toggle(userId, "video-1", FavoriteContentType.VIDEO));
        then(favoriteRepository).should(never()).save(any());
        then(favoriteRepository).should(never()).delete(any());
    }

    @Test
    void shouldListAllFavorites() {
        UserFavorite f1 = UserFavorite.builder()
                .user(user).itemId("video-1")
                .itemType(FavoriteContentType.VIDEO).build();
        ReflectionTestUtils.setField(f1, "createdAt", LocalDateTime.now());

        UserFavorite f2 = UserFavorite.builder()
                .user(user).itemId("menu-1")
                .itemType(FavoriteContentType.MENU).build();
        ReflectionTestUtils.setField(f2, "createdAt", LocalDateTime.now());

        given(favoriteRepository.findByUser_Id(userId)).willReturn(List.of(f1, f2));

        List<FavoriteDTO> result = favoriteService.listAll(userId);

        assertEquals(2, result.size());
        assertEquals("video-1", result.get(0).itemId());
        assertEquals(FavoriteContentType.VIDEO, result.get(0).itemType());
        assertEquals("menu-1", result.get(1).itemId());
        assertEquals(FavoriteContentType.MENU, result.get(1).itemType());
    }

    @Test
    void shouldListFavoritesByType() {
        UserFavorite f1 = UserFavorite.builder()
                .user(user).itemId("video-1")
                .itemType(FavoriteContentType.VIDEO).build();
        ReflectionTestUtils.setField(f1, "createdAt", LocalDateTime.now());

        given(favoriteRepository.findByUser_IdAndItemType(userId, FavoriteContentType.VIDEO))
                .willReturn(List.of(f1));

        List<FavoriteDTO> result = favoriteService.listByType(userId, FavoriteContentType.VIDEO);

        assertEquals(1, result.size());
        assertEquals("video-1", result.get(0).itemId());
        assertEquals(FavoriteContentType.VIDEO, result.get(0).itemType());
    }

    @Test
    void shouldReturnEmptyListWhenNoFavorites() {
        given(favoriteRepository.findByUser_Id(userId)).willReturn(List.of());

        List<FavoriteDTO> result = favoriteService.listAll(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnTrueWhenIsFavorited() {
        given(favoriteRepository.existsByUser_IdAndItemIdAndItemType(
                userId, "video-1", FavoriteContentType.VIDEO)).willReturn(true);

        assertTrue(favoriteService.isFavorited(userId, "video-1", FavoriteContentType.VIDEO));
    }

    @Test
    void shouldReturnFalseWhenNotFavorited() {
        given(favoriteRepository.existsByUser_IdAndItemIdAndItemType(
                userId, "video-1", FavoriteContentType.VIDEO)).willReturn(false);

        assertFalse(favoriteService.isFavorited(userId, "video-1", FavoriteContentType.VIDEO));
    }

    @Test
    void shouldCountLikes() {
        given(favoriteRepository.countByItemIdAndItemType(
                "video-1", FavoriteContentType.VIDEO)).willReturn(5L);

        assertEquals(5L, favoriteService.countLikes("video-1", FavoriteContentType.VIDEO));
    }

    @Test
    void shouldReturnZeroLikesForUnknownItem() {
        given(favoriteRepository.countByItemIdAndItemType(
                "unknown", FavoriteContentType.VIDEO)).willReturn(0L);

        assertEquals(0L, favoriteService.countLikes("unknown", FavoriteContentType.VIDEO));
    }
}
