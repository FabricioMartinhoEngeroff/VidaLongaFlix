package com.dvFabricio.VidaLongaFlix.integration.auth;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import com.dvFabricio.VidaLongaFlix.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de isolamento entre usuários:
 * <p>
 * - Favoritos de User A não aparecem para User B
 * - Cada usuário só vê seus próprios favoritos
 * - Contagem de likes é global (não por usuário)
 * - GAP DOCUMENTADO: DELETE /comments/{id} não exige autenticação nem
 *   verifica ownership — qualquer um (inclusive sem token) pode deletar
 *   qualquer comentário. Recomendação: proteger o endpoint e adicionar
 *   verificação de dono no serviço.
 */
class UserIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String USER_B_EMAIL = "isolation-userB@integration.test";

    private String adminToken;   // User A
    private String userBToken;   // User B
    private UUID videoId;
    private UUID categoryId;

    @BeforeEach
    void setUp() throws Exception {
        // User A = admin já existente
        adminToken = getAdminToken();

        // User B = criado via repositório para contornar o bug de registro (taxId)
        if (!userRepository.existsByEmail(USER_B_EMAIL)) {
            Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
            User userB = new User(
                    "Isolation User B", USER_B_EMAIL,
                    passwordEncoder.encode("IsoTest@1234"), "(51) 88888-0002");
            userB.setTaxId("222.222.222-" + UUID.randomUUID().toString().substring(0, 2));
            userB.setRoles(List.of(userRole));
            userRepository.save(userB);
        }
        userBToken = loginAs(USER_B_EMAIL, "IsoTest@1234");

        // Cria categoria e vídeo para os testes de favorito/comentário
        Category category = categoryRepository.save(
                new Category("Iso-Cat-" + UUID.randomUUID(), CategoryType.VIDEO));
        categoryId = category.getId();

        Video video = Video.builder()
                .title("Iso-Video-" + UUID.randomUUID())
                .description("Vídeo para testes de isolamento")
                .url("https://youtube.com/iso-test")
                .cover("https://img.test/iso-cover.jpg")
                .category(category)
                .build();
        videoRepository.save(video);
        videoId = video.getId();
    }

    @AfterEach
    void cleanup() {
        commentRepository.findByVideo_Id(videoId).forEach(commentRepository::delete);

        userRepository.findByEmail(USER_B_EMAIL).ifPresent(userB -> {
            favoriteRepository.findByUser_Id(userB.getId()).forEach(favoriteRepository::delete);
            userRepository.delete(userB);
        });

        userRepository.findByEmail("admin@vidalongaflix.com").ifPresent(admin ->
                favoriteRepository.findByUser_Id(admin.getId()).stream()
                        .filter(f -> f.getItemId().equals(videoId.toString()))
                        .forEach(favoriteRepository::delete)
        );

        videoRepository.deleteById(videoId);
        categoryRepository.deleteById(categoryId);
    }

    // ─────────────────── ISOLAMENTO DE FAVORITOS ──────────────────────────

    @Test
    void shouldNotExposeUserAFavoritesToUserB() throws Exception {
        // User A favorita o vídeo
        mockMvc.perform(bearer(
                post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                adminToken));

        // User B lista seus favoritos — não deve ver o favorito do User A
        mockMvc.perform(bearer(get("/favorites"), userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.itemId == '" + videoId + "')]").doesNotExist());
    }

    @Test
    void shouldNotExposeUserBFavoritesToUserA() throws Exception {
        // User B favorita o vídeo
        mockMvc.perform(bearer(
                post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                userBToken));

        // User A lista seus favoritos — não deve ver o favorito do User B
        mockMvc.perform(bearer(get("/favorites"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.itemId == '" + videoId + "')]").doesNotExist());
    }

    @Test
    void shouldCountLikesFromBothUsersIndependently() throws Exception {
        // User A e User B favoritam o mesmo item
        mockMvc.perform(bearer(
                post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                adminToken));
        mockMvc.perform(bearer(
                post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                userBToken));

        // Status de likes é global — deve contar os dois
        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status", FavoriteContentType.VIDEO, videoId),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(2))
                .andExpect(jsonPath("$.favorited").value(true));

        // User B também vê likesCount = 2, mas favorited = true para ele
        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status", FavoriteContentType.VIDEO, videoId),
                        userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(2))
                .andExpect(jsonPath("$.favorited").value(true));
    }

    @Test
    void shouldShowFavoritedTrueOnlyForUserWhoFavorited() throws Exception {
        // Só o User A favorita
        mockMvc.perform(bearer(
                post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                adminToken));

        // User A: favorited = true
        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status", FavoriteContentType.VIDEO, videoId),
                        adminToken))
                .andExpect(jsonPath("$.favorited").value(true));

        // User B: favorited = false (não favoritou)
        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status", FavoriteContentType.VIDEO, videoId),
                        userBToken))
                .andExpect(jsonPath("$.favorited").value(false));
    }

    // ─────────────────── OWNERSHIP DE COMENTÁRIOS ─────────────────────────

    @Test
    void shouldBlockCommentDeletionWithoutToken() throws Exception {
        // User A cria um comentário
        String body = String.format("{\"text\":\"Comentário do User A\",\"videoId\":\"%s\"}", videoId);
        mockMvc.perform(bearer(post("/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body),
                        adminToken))
                .andExpect(status().isCreated());

        UUID commentId = commentRepository.findByVideo_Id(videoId)
                .stream().findFirst().orElseThrow().getId();

        // Sem token → 403
        mockMvc.perform(delete("/comments/{id}", commentId))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBlockCommentDeletionForNonAdminUser() throws Exception {
        // User A (admin) cria um comentário
        String body = String.format("{\"text\":\"Comentário do Admin\",\"videoId\":\"%s\"}", videoId);
        mockMvc.perform(bearer(post("/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body),
                        adminToken))
                .andExpect(status().isCreated());

        UUID commentId = commentRepository.findByVideo_Id(videoId)
                .stream().findFirst().orElseThrow().getId();

        // User B (ROLE_USER) tenta deletar → 403
        mockMvc.perform(bearer(delete("/comments/{id}", commentId), userBToken))
                .andExpect(status().isForbidden());

        // Confirma que o comentário ainda existe
        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldAllowAdminToDeleteAnyComment() throws Exception {
        // User B cria um comentário
        String body = String.format("{\"text\":\"Comentário do User B\",\"videoId\":\"%s\"}", videoId);
        mockMvc.perform(bearer(post("/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body),
                        userBToken))
                .andExpect(status().isCreated());

        UUID commentId = commentRepository.findByVideo_Id(videoId)
                .stream().findFirst().orElseThrow().getId();

        // Admin deleta o comentário do User B → 204
        mockMvc.perform(bearer(delete("/comments/{id}", commentId), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
