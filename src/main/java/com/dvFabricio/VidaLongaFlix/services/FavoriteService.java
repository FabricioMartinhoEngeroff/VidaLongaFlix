package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteDTO;
import com.dvFabricio.VidaLongaFlix.domain.favorite.UserFavorite;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.FavoriteRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    // Toggle: se já favoritou, remove — se não, adiciona
    @Transactional
    public boolean toggle(UUID userId, String itemId, FavoriteContentType itemType) {
        User user = findUser(userId);

        Optional<UserFavorite> existing = favoriteRepository
                .findByUser_IdAndItemIdAndFavoriteContentType(userId, itemId, itemType);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false; // removido
        }

        UserFavorite favorite = UserFavorite.builder()
                .user(user)
                .itemId(itemId)
                .itemType(itemType)
                .build();

        favoriteRepository.save(favorite);
        return true; // adicionado
    }

    // Lista todos os favoritos do usuário
    public List<FavoriteDTO> listAll(UUID userId) {
        return favoriteRepository.findByUser_Id(userId)
                .stream()
                .map(FavoriteDTO::new)
                .toList();
    }

    // Lista favoritos por tipo (VIDEO, MENU, RECIPE, etc.)
    public List<FavoriteDTO> listByType(UUID userId, FavoriteContentType itemType) {
        return favoriteRepository.findByUser_IdAndFavoriteContentType(userId, itemType)
                .stream()
                .map(FavoriteDTO::new)
                .toList();
    }

    // Verifica se um item específico está favoritado
    public boolean isFavorited(UUID userId, String itemId, FavoriteContentType itemType) {
        return favoriteRepository
                .existsByUser_IdAndItemIdAndFavoriteContentType(userId, itemId, itemType);
    }

    // Conta quantos usuários favoritaram um item (likes count)
    public long countLikes(String itemId, FavoriteContentType itemType) {
        return favoriteRepository.countByItemIdAndFavoriteContentType(itemId, itemType);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "User with ID " + userId + " not found."));
    }
}