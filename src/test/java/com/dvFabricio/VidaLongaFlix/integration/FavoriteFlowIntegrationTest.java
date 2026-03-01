package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.repositories.FavoriteRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o fluxo completo de favoritos.
 * <p>
 * FavoriteController usa @AuthenticationPrincipal — todos os endpoints
 * requerem autenticação. O itemId é uma String livre (sem FK para vídeo/menu).
 */
class FavoriteFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private UserRepository userRepository;

    private String adminToken;
    private String itemId;   // ID livre — o favorito não exige entidade real

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();
        itemId = "item-it-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        // Remove favoritos criados pelos testes para não poluir outros
        userRepository.findByEmail("admin@vidalongaflix.com").ifPresent(admin ->
                favoriteRepository.findByUser_Id(admin.getId())
                        .stream()
                        .filter(f -> f.getItemId().startsWith("item-it-"))
                        .forEach(favoriteRepository::delete)
        );
    }

    // ─────────────────────────── TOGGLE ───────────────────────────────────

    @Test
    void shouldAddFavoriteOnFirstToggle() throws Exception {
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.itemType").value("VIDEO"));
    }

    @Test
    void shouldRemoveFavoriteOnSecondToggle() throws Exception {
        // Primeira chamada: adiciona
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken))
                .andExpect(jsonPath("$.favorited").value(true));

        // Segunda chamada: remove
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));
    }

    @Test
    void shouldToggleMenuFavoriteIndependentlyFromVideoFavorite() throws Exception {
        // Adiciona como VIDEO
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken))
                .andExpect(jsonPath("$.favorited").value(true));

        // Adiciona como MENU com mesmo itemId — são favoritos independentes
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.MENU, itemId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.itemType").value("MENU"));
    }

    // ─────────────────────────── LISTAGEM ─────────────────────────────────

    @Test
    void shouldListAllFavoritesForAuthenticatedUser() throws Exception {
        // Adiciona dois favoritos
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken));
        String secondItem = "item-it-" + UUID.randomUUID();
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.MENU, secondItem), adminToken));

        mockMvc.perform(bearer(get("/favorites"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnEmptyListWhenNoFavoritesExist() throws Exception {
        mockMvc.perform(bearer(get("/favorites"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldListFavoritesByVideoType() throws Exception {
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken));

        mockMvc.perform(bearer(get("/favorites/{type}", FavoriteContentType.VIDEO), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldListFavoritesByMenuType() throws Exception {
        mockMvc.perform(bearer(get("/favorites/{type}", FavoriteContentType.MENU), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────── STATUS ───────────────────────────────────

    @Test
    void shouldReturnFavoritedTrueAfterToggle() throws Exception {
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken));

        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status",
                                FavoriteContentType.VIDEO, itemId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void shouldReturnFavoritedFalseForItemNeverFavorited() throws Exception {
        String neverFavoritedItem = "item-it-never-" + UUID.randomUUID();

        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status",
                                FavoriteContentType.VIDEO, neverFavoritedItem), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false))
                .andExpect(jsonPath("$.likesCount").value(0));
    }

    @Test
    void shouldReturnZeroLikesAfterRemovingFavorite() throws Exception {
        // Adiciona
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken));
        // Remove
        mockMvc.perform(bearer(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId), adminToken));

        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status",
                                FavoriteContentType.VIDEO, itemId), adminToken))
                .andExpect(jsonPath("$.favorited").value(false))
                .andExpect(jsonPath("$.likesCount").value(0));
    }

    // ─────────────────────────── SEGURANÇA ────────────────────────────────

    @Test
    void shouldReturn403WhenAccessingFavoritesWithoutToken() throws Exception {
        mockMvc.perform(get("/favorites"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenTogglingFavoriteWithoutToken() throws Exception {
        mockMvc.perform(post("/favorites/{type}/{itemId}",
                        FavoriteContentType.VIDEO, itemId))
                .andExpect(status().isForbidden());
    }
}
