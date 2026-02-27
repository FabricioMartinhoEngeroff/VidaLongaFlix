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
 * Testes de integração para o fluxo completo de categorias:
 * GET, POST, PUT, DELETE e validação de duplicatas.
 * <p>
 * O endpoint /categories/** é permitAll, portanto não exige token.
 */
class CategoryFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;

    private String uniqueName;
    private UUID createdCategoryId;

    @BeforeEach
    void setUp() {
        uniqueName = "Cat-IT-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        // Remove categorias criadas nos testes
        categoryRepository.findAll().stream()
                .filter(c -> c.getName().startsWith("Cat-IT-"))
                .forEach(categoryRepository::delete);
    }

    // ─────────────────────────── GET ──────────────────────────────────────

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

    // ─────────────────────────── POST ─────────────────────────────────────

    @Test
    void shouldCreateVideoCategorySuccessfully() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO(uniqueName, CategoryType.VIDEO);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verifica persistência
        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(jsonPath("$[*].name", hasItem(uniqueName)));
    }

    @Test
    void shouldCreateMenuCategorySuccessfully() throws Exception {
        String menuName = "Cat-IT-MENU-NEW-" + UUID.randomUUID();
        CategoryRequestDTO request = new CategoryRequestDTO(menuName, CategoryType.MENU);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnConflictForDuplicateCategoryNameAndType() throws Exception {
        // Cria a categoria uma primeira vez
        categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        // Tenta criar novamente com mesmo nome e tipo
        CategoryRequestDTO request = new CategoryRequestDTO(uniqueName, CategoryType.VIDEO);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO("", CategoryType.VIDEO);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenTypeIsNull() throws Exception {
        String body = "{\"name\":\"Sem Tipo\"}";

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── PUT ──────────────────────────────────────

    @Test
    void shouldUpdateCategoryName() throws Exception {
        Category saved = categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        String updatedName = uniqueName + "-UPDATED";
        CategoryRequestDTO update = new CategoryRequestDTO(updatedName, CategoryType.VIDEO);

        mockMvc.perform(put("/categories/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNoContent());

        // Verifica que o nome foi atualizado
        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(jsonPath("$[*].name", hasItem(updatedName)));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCategory() throws Exception {
        CategoryRequestDTO update = new CategoryRequestDTO("Qualquer", CategoryType.VIDEO);

        mockMvc.perform(put("/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────── DELETE ───────────────────────────────────

    @Test
    void shouldDeleteCategorySuccessfully() throws Exception {
        Category saved = categoryRepository.save(new Category(uniqueName, CategoryType.VIDEO));

        mockMvc.perform(delete("/categories/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verifica que não existe mais na listagem
        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(jsonPath("$[*].name").value(
                        org.hamcrest.Matchers.not(hasItem(uniqueName))));
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCategory() throws Exception {
        mockMvc.perform(delete("/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
