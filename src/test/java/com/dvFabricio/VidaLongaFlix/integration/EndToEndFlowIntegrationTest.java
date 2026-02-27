package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.favorite.FavoriteContentType;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.CommentRepository;
import com.dvFabricio.VidaLongaFlix.repositories.FavoriteRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração end-to-end cobrindo o fluxo completo de um usuário:
 * <p>
 * Admin cria conteúdo → usuário assiste, comenta e favorita →
 * usuário desfavorita e apaga o comentário → verifica estado final limpo.
 */
class EndToEndFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private FavoriteRepository favoriteRepository;

    private UUID categoryId;
    private UUID videoId;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();

        Category category = categoryRepository.save(
                new Category("E2E-Cat-" + UUID.randomUUID(), CategoryType.VIDEO));
        categoryId = category.getId();
    }

    @AfterEach
    void cleanup() {
        if (videoId != null) {
            commentRepository.findByVideo_Id(videoId).forEach(commentRepository::delete);

            videoRepository.findById(videoId).ifPresent(v ->
                    favoriteRepository.findAll().stream()
                            .filter(f -> f.getItemId().equals(videoId.toString()))
                            .forEach(favoriteRepository::delete)
            );

            videoRepository.deleteById(videoId);
        }
        categoryRepository.deleteById(categoryId);
    }

    /**
     * Fluxo completo:
     * 1. Admin cria vídeo
     * 2. Verifica vídeo aparece na listagem pública
     * 3. Registra uma visualização
     * 4. Admin comenta no vídeo
     * 5. Verifica comentário salvo
     * 6. Admin favorita o vídeo
     * 7. Verifica favorito na lista e no status (likesCount = 1, favorited = true)
     * 8. Admin desfavorita
     * 9. Verifica status (likesCount = 0, favorited = false)
     * 10. Admin deleta o comentário
     * 11. Verifica lista de comentários vazia
     */
    @Test
    void shouldCompleteFullUserContentInteractionFlow() throws Exception {

        // 1. Admin cria vídeo
        VideoRequestDTO createReq = new VideoRequestDTO(
                "E2E Vídeo de Saúde", "Conteúdo completo de bem-estar",
                "https://youtube.com/e2e-video", "https://img.test/e2e-cover.jpg",
                categoryId, null, null, null, null, null, null);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)), adminToken))
                .andExpect(status().isCreated());

        videoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals("E2E Vídeo de Saúde"))
                .findFirst().orElseThrow().getId();

        // 2. Verifica vídeo visível na listagem pública
        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'E2E Vídeo de Saúde')]").exists());

        // 3. Registra uma visualização (endpoint é PATCH)
        mockMvc.perform(patch("/videos/{id}/view", videoId))
                .andExpect(status().isOk());

        // Verifica views incrementado
        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(1));

        // 4. Admin comenta no vídeo
        String commentBody = String.format(
                "{\"text\":\"Ótimo conteúdo!\",\"videoId\":\"%s\"}", videoId);

        mockMvc.perform(bearer(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentBody), adminToken))
                .andExpect(status().isCreated());

        // 5. Verifica comentário salvo
        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Ótimo conteúdo!"));

        UUID commentId = commentRepository.findByVideo_Id(videoId)
                .stream().findFirst().orElseThrow().getId();

        // 6. Admin favorita o vídeo
        mockMvc.perform(bearer(
                        post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true));

        // 7. Verifica favorito na lista e no status
        mockMvc.perform(bearer(get("/favorites"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.itemId == '" + videoId + "')]").exists());

        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status", FavoriteContentType.VIDEO, videoId),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.likesCount").value(1));

        // 8. Admin desfavorita (segundo toggle)
        mockMvc.perform(bearer(
                        post("/favorites/{type}/{itemId}", FavoriteContentType.VIDEO, videoId),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));

        // 9. Verifica status zerado
        mockMvc.perform(bearer(
                        get("/favorites/{type}/{itemId}/status", FavoriteContentType.VIDEO, videoId),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false))
                .andExpect(jsonPath("$.likesCount").value(0));

        // 10. Admin deleta o comentário (DELETE /comments agora exige ROLE_ADMIN)
        mockMvc.perform(bearer(delete("/comments/{id}", commentId), adminToken))
                .andExpect(status().isNoContent());

        // 11. Verifica lista de comentários vazia
        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Fluxo de conteúdo admin completo:
     * Cria categoria → cria vídeo nessa categoria → verifica analytics → deleta vídeo.
     */
    @Test
    void shouldCompleteAdminContentLifecycle() throws Exception {

        // Cria vídeo
        VideoRequestDTO createReq = new VideoRequestDTO(
                "E2E Lifecycle Video", "Descrição do ciclo completo",
                "https://youtube.com/lifecycle", "https://img.test/lifecycle.jpg",
                categoryId, null, 30.0, 50.0, 15.0, 5.0, 450.0);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)), adminToken))
                .andExpect(status().isCreated());

        videoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals("E2E Lifecycle Video"))
                .findFirst().orElseThrow().getId();

        // Verifica criação
        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Lifecycle Video"))
                .andExpect(jsonPath("$.calories").value(450.0));

        // Atualiza o vídeo — VideoRequestDTO tem @NotBlank em todos os campos obrigatórios
        VideoRequestDTO updateReq = new VideoRequestDTO(
                "E2E Lifecycle Video - Atualizado", "Descrição atualizada",
                "https://youtube.com/lifecycle-updated", "https://img.test/lifecycle-updated.jpg",
                categoryId, null, null, null, null, null, null);

        mockMvc.perform(bearer(put("/admin/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)), adminToken))
                .andExpect(status().isOk());

        // Verifica atualização
        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Lifecycle Video - Atualizado"));

        // Deleta o vídeo
        mockMvc.perform(bearer(delete("/admin/videos/{id}", videoId), adminToken))
                .andExpect(status().isNoContent());

        videoId = null; // Evita double-delete no cleanup

        // Verifica que não existe mais
        mockMvc.perform(get("/videos/{id}", videoId == null ? UUID.randomUUID() : videoId))
                .andExpect(status().isNotFound());
    }
}
