package com.dvFabricio.VidaLongaFlix.favoriteTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.UserFavorite;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FavoriteDomainTest {

    @Test
    void shouldCreateFavoriteWithBuilder() {
        User user = new User("João", "joao@example.com", "Password1@", "(11) 99999-9999");

        UserFavorite favorite = UserFavorite.builder()
                .user(user)
                .itemId("video-123")
                .itemType(FavoriteContentType.VIDEO)
                .build();

        assertEquals("video-123", favorite.getItemId());
        assertEquals(FavoriteContentType.VIDEO, favorite.getItemType()); // ← getItemType()
        assertEquals(user, favorite.getUser());
        assertNull(favorite.getId());
        assertNull(favorite.getCreatedAt());
    }

    @Test
    void shouldSupportAllFavoriteContentTypes() {
        for (FavoriteContentType type : FavoriteContentType.values()) {
            UserFavorite favorite = UserFavorite.builder()
                    .itemId("item-1")
                    .itemType(type)
                    .build();
            assertEquals(type, favorite.getItemType()); // ← getItemType()
        }
    }

    @Test
    void shouldSetCreatedAtOnPrePersist() {
        UserFavorite favorite = new UserFavorite();
        favorite.onCreate();
        assertNotNull(favorite.getCreatedAt());
    }
}