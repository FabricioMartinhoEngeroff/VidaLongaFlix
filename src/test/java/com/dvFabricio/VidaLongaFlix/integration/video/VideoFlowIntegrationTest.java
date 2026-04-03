package com.dvFabricio.VidaLongaFlix.integration.video;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o fluxo completo de vídeos:
 * leitura pública, criação/edição/exclusão via admin, registro de views e analytics.
 */
class VideoFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;

    private String adminToken;
    private UUID categoryId;
    private String videoTitle;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();

        Category category = categoryRepository.save(
                new Category("Video-Flow-Cat-" + UUID.randomUUID(), CategoryType.VIDEO));
        categoryId = category.getId();

        // Título único por execução evita conflito entre testes
        videoTitle = "Video-IT-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        // Vídeos devem ser excluídos antes da categoria (FK)
        videoRepository.findAll().stream()
                .filter(v -> v.getTitle().startsWith("Video-IT-"))
                .forEach(videoRepository::delete);
        categoryRepository.deleteById(categoryId);
    }

    // ─────────────────────────── LEITURA PÚBLICA ──────────────────────────

    @Test
    void shouldReturnEmptyVideoListWhenNoneExist() throws Exception {
        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn404ForNonExistentVideo() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/videos/{id}", randomId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnMostViewedVideosEndpoint() throws Exception {
        mockMvc.perform(get("/videos/most-viewed").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnLeastViewedVideosEndpoint() throws Exception {
        mockMvc.perform(get("/videos/least-viewed").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnViewsByCategoryEndpoint() throws Exception {
        mockMvc.perform(get("/videos/views-by-category"))
                .andExpect(status().isOk());
    }

    // ─────────────────────────── CRUD ADMIN ───────────────────────────────

    @Test
    void shouldCreateVideoAsAdmin() throws Exception {
        VideoRequestDTO request = buildVideoRequest(videoTitle, categoryId);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        // Verifica que o vídeo foi persistido
        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(videoTitle)));
    }

    @Test
    void shouldGetCreatedVideoById() throws Exception {
        // Cria o vídeo
        VideoRequestDTO request = buildVideoRequest(videoTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        // Obtém o ID do vídeo criado via repositório
        UUID videoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals(videoTitle))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(videoTitle))
                .andExpect(jsonPath("$.views").value(0));
    }

    @Test
    void shouldUpdateVideoAsAdmin() throws Exception {
        // Cria o vídeo
        VideoRequestDTO create = buildVideoRequest(videoTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)), adminToken))
                .andExpect(status().isCreated());

        UUID videoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals(videoTitle))
                .findFirst().orElseThrow().getId();

        String updatedTitle = videoTitle + "-UPDATED";
        VideoRequestDTO update = buildVideoRequest(updatedTitle, categoryId);

        mockMvc.perform(bearer(put("/admin/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle));
    }

    @Test
    void shouldRegisterVideoView() throws Exception {
        // Cria o vídeo
        VideoRequestDTO request = buildVideoRequest(videoTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        UUID videoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals(videoTitle))
                .findFirst().orElseThrow().getId();

        // Registra view (endpoint público — não precisa de token)
        mockMvc.perform(patch("/videos/{id}/view", videoId))
                .andExpect(status().isOk());

        // Confirma que views foi incrementado
        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(1));
    }

    @Test
    void shouldDeleteVideoAsAdmin() throws Exception {
        // Cria o vídeo
        VideoRequestDTO request = buildVideoRequest(videoTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        UUID videoId = videoRepository.findAll().stream()
                .filter(v -> v.getTitle().equals(videoTitle))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(bearer(delete("/admin/videos/{id}", videoId), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentVideo() throws Exception {
        VideoRequestDTO request = buildVideoRequest(videoTitle, categoryId);
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(bearer(put("/admin/videos/{id}", randomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentVideo() throws Exception {
        mockMvc.perform(bearer(delete("/admin/videos/{id}", UUID.randomUUID()), adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenCreatingVideoWithMissingFields() throws Exception {
        // Título vazio viola @NotBlank
        String invalidJson = String.format(
                "{\"title\":\"\",\"description\":\"Desc\",\"url\":\"http://u\","
                        + "\"cover\":\"http://c\",\"categoryId\":\"%s\"}", categoryId);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson), adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlobVideoUrlFromAdminCreate() throws Exception {
        String invalidJson = String.format(
                "{\"title\":\"Video inválido\",\"description\":\"Desc\",\"url\":\"blob:https://vidalongaflix.com/123\","
                        + "\"cover\":\"https://cdn.example.com/capa.jpg\",\"categoryId\":\"%s\"}", categoryId);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson), adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("url"));
    }

    @Test
    void shouldRejectLocalCoverPathFromAdminCreate() throws Exception {
        String invalidJson = String.format(
                "{\"title\":\"Video inválido\",\"description\":\"Desc\",\"url\":\"https://cdn.example.com/video.mp4\","
                        + "\"cover\":\"C:/Users/Fabricio/Downloads/capa.jpg\",\"categoryId\":\"%s\"}", categoryId);

        mockMvc.perform(bearer(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson), adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("cover"));
    }

    // ─────────────────────────── HELPERS ──────────────────────────────────

    private VideoRequestDTO buildVideoRequest(String title, UUID catId) {
        return new VideoRequestDTO(
                title, "Descrição de integração",
                "https://youtube.com/watch?v=test",
                "https://img.youtube.com/vi/test/0.jpg",
                catId, null, null, null, null, null, null);
    }
}
