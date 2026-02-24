package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<UserFavorite, UUID> {

    List<UserFavorite> findByUser_Id(UUID userId);

    List<UserFavorite> findByUser_IdAndFavoriteContentType(UUID userId, FavoriteContentType itemType);

    Optional<UserFavorite> findByUser_IdAndItemIdAndFavoriteContentType(
            UUID userId, String itemId, FavoriteContentType itemType);

    boolean existsByUser_IdAndItemIdAndFavoriteContentType(
            UUID userId, String itemId, FavoriteContentType itemType);

    long countByItemIdAndFavoriteContentType(String itemId, FavoriteContentType itemType);
}
