package com.dvFabricio.VidaLongaFlix.domain.favorite;

import java.time.LocalDateTime;

public record FavoriteDTO(
        String itemId,
        FavoriteContentType itemType,
        LocalDateTime createdAt
) {
    public static FavoriteDTO from(UserFavorite favorite) {
        return new FavoriteDTO(
                favorite.getItemId(),
                favorite.getItemType(),
                favorite.getCreatedAt()
        );
    }
}
