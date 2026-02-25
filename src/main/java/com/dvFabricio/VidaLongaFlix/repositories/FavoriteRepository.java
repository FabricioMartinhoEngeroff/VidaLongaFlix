package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<UserFavorite, UUID> {

    List<UserFavorite> findByUser_Id(UUID userId);

    List<UserFavorite> findByUser_IdAndItemType(UUID userId, FavoriteContentType itemType);

    Optional<UserFavorite> findByUser_IdAndItemIdAndItemType(
            UUID userId, String itemId, FavoriteContentType itemType);

    boolean existsByUser_IdAndItemIdAndItemType(
            UUID userId, String itemId, FavoriteContentType itemType);

    long countByItemIdAndItemType(String itemId, FavoriteContentType itemType);
}