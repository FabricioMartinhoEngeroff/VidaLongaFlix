package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
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
 * Testes de integração para o fluxo completo de categorias.
 * <p>
 * GET /categories → público (sem token)
 * POST/PUT/DELETE /categories → somente ROLE_ADMIN
 */
class CategoryFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;

    private String adminToken;
    private String uniqueName;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();
        uniqueName = "Cat-IT-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        categoryRepository.findAll().stream()
                .filter(c -> c.getName().startsWith("Cat-IT-"))
                .forEach(categoryRepository::delete);
    }

    // ─────────────────────────── GET (público) ────────────────────────────

    @Test
    void shouldReturnCategoriesByVideoType() throws Exception {
        categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].name", hasItem(uniqueName)));
    }

    @Test
    void shouldReturnCategoriesByMenuType() throws Exception {
        String menuCatName = "Cat-IT-MENU-" + UUID.randomUUID();
        categoryRepository.save(new Category(menuCatName, CategoryType.MENU));

        mockMvc.perform(get("/categories").param("type", "MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(menuCatName)));
    }

    @Test
    void shouldReturnBadRequestWhenTypeParamIsMissing() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── POST (só admin) ──────────────────────────

    @Test
    void shouldCreateVideoCategorySuccessfully() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO(uniqueName, CategoryType.VIDEO);

        mockMvc.perform(bearer(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(jsonPath("$[*].name", hasItem(uniqueName)));
    }

    @Test
    void shouldCreateMenuCategorySuccessfully() throws Exception {
        String menuName = "Cat-IT-MENU-NEW-" + UUID.randomUUID();
        CategoryRequestDTO request = new CategoryRequestDTO(menuName, CategoryType.MENU);

        mockMvc.perform(bearer(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn403WhenCreatingCategoryWithoutToken() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO(uniqueName, CategoryType.VIDEO);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnConflictForDuplicateCategoryNameAndType() throws Exception {
        categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        CategoryRequestDTO request = new CategoryRequestDTO(uniqueName, CategoryType.VIDEO);

        mockMvc.perform(bearer(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO("", CategoryType.VIDEO);

        mockMvc.perform(bearer(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenTypeIsNull() throws Exception {
        String body = "{\"name\":\"Sem Tipo\"}";

        mockMvc.perform(bearer(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body), adminToken))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── PUT (só admin) ───────────────────────────

    @Test
    void shouldUpdateCategoryName() throws Exception {
        Category saved = categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        String updatedName = uniqueName + "-UPDATED";
        CategoryRequestDTO update = new CategoryRequestDTO(updatedName, CategoryType.VIDEO);

        mockMvc.perform(bearer(put("/categories/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(jsonPath("$[*].name", hasItem(updatedName)));
    }

    @Test
    void shouldReturn403WhenUpdatingCategoryWithoutToken() throws Exception {
        Category saved = categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));
        CategoryRequestDTO update = new CategoryRequestDTO(uniqueName + "-X", CategoryType.VIDEO);

        mockMvc.perform(put("/categories/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCategory() throws Exception {
        CategoryRequestDTO update = new CategoryRequestDTO("Qualquer", CategoryType.VIDEO);

        mockMvc.perform(bearer(put("/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)), adminToken))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────── DELETE (só admin) ────────────────────────

    @Test
    void shouldDeleteCategorySuccessfully() throws Exception {
        Category saved = categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        mockMvc.perform(bearer(delete("/categories/{id}", saved.getId()), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(jsonPath("$[*].name").value(
                        org.hamcrest.Matchers.not(hasItem(uniqueName))));
    }

    @Test
    void shouldReturn403WhenDeletingCategoryWithoutToken() throws Exception {
        Category saved = categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        mockMvc.perform(delete("/categories/{id}", saved.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCategory() throws Exception {
        mockMvc.perform(bearer(delete("/categories/{id}", UUID.randomUUID()), adminToken))
                .andExpect(status().isNotFound());
    }
}
