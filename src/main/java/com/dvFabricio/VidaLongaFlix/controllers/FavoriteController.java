package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.services.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // Toggle favorito — funciona para qualquer FavoriteContentType
    @PostMapping("/{type}/{itemId}")
    public ResponseEntity<Map<String, Object>> toggle(
            @AuthenticationPrincipal User user,
            @PathVariable FavoriteContentType type,
            @PathVariable String itemId) {

        boolean added = favoriteService.toggle(user.getId(), itemId, type);
        return ResponseEntity.ok(Map.of(
                "favorited", added,
                "itemId", itemId,
                "itemType", type
        ));
    }

    // Lista todos os favoritos do usuário (videos + menus + futuros)
    @GetMapping
    public ResponseEntity<List<FavoriteDTO>> listAll(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(favoriteService.listAll(user.getId()));
    }

    // Lista favoritos por tipo
    @GetMapping("/{type}")
    public ResponseEntity<List<FavoriteDTO>> listByType(
            @AuthenticationPrincipal User user,
            @PathVariable FavoriteContentType type) {
        return ResponseEntity.ok(
                favoriteService.listByType(user.getId(), type));
    }

    // Verifica se um item está favoritado
    @GetMapping("/{type}/{itemId}/status")
    public ResponseEntity<Map<String, Object>> status(
            @AuthenticationPrincipal User user,
            @PathVariable FavoriteContentType type,
            @PathVariable String itemId) {

        boolean favorited = favoriteService.isFavorited(
                user.getId(), itemId, type);
        long likes = favoriteService.countLikes(itemId, type);

        return ResponseEntity.ok(Map.of(
                "favorited", favorited,
                "likesCount", likes
        ));
    }
}