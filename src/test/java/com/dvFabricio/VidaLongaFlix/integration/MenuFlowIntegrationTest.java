package com.dvFabricio.VidaLongaFlix.integration;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuRequestDTO;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
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
 * Testes de integração para o fluxo completo de menus/receitas:
 * leitura pública, criação/edição/exclusão via admin.
 */
class MenuFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MenuRepository menuRepository;

    private String adminToken;
    private UUID categoryId;
    private String menuTitle;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();

        Category category = categoryRepository.save(
                new Category("Menu-Flow-Cat-" + UUID.randomUUID(), CategoryType.MENU));
        categoryId = category.getId();

        menuTitle = "Menu-IT-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        menuRepository.findAll().stream()
                .filter(m -> m.getTitle().startsWith("Menu-IT-"))
                .forEach(menuRepository::delete);
        categoryRepository.deleteById(categoryId);
    }

    // ─────────────────────────── LEITURA PÚBLICA ──────────────────────────

    @Test
    void shouldReturnEmptyMenuListWhenNoneExist() throws Exception {
        mockMvc.perform(get("/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn404ForNonExistentMenu() throws Exception {
        mockMvc.perform(get("/menus/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────── CRUD ADMIN ───────────────────────────────

    @Test
    void shouldCreateMenuAsAdmin() throws Exception {
        MenuRequestDTO request = buildMenuRequest(menuTitle, categoryId);

        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(menuTitle)));
    }

    @Test
    void shouldGetCreatedMenuById() throws Exception {
        MenuRequestDTO request = buildMenuRequest(menuTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        UUID menuId = menuRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(menuTitle))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(get("/menus/{id}", menuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(menuTitle))
                .andExpect(jsonPath("$.description").value("Receita de integração"))
                .andExpect(jsonPath("$.protein").value(25.0))
                .andExpect(jsonPath("$.calories").value(300.0));
    }

    @Test
    void shouldUpdateMenuAsAdmin() throws Exception {
        MenuRequestDTO create = buildMenuRequest(menuTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)), adminToken))
                .andExpect(status().isCreated());

        UUID menuId = menuRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(menuTitle))
                .findFirst().orElseThrow().getId();

        String updatedTitle = menuTitle + "-UPDATED";
        MenuRequestDTO update = buildMenuRequest(updatedTitle, categoryId);

        mockMvc.perform(bearer(put("/admin/menus/{id}", menuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle));
    }

    @Test
    void shouldDeleteMenuAsAdmin() throws Exception {
        MenuRequestDTO request = buildMenuRequest(menuTitle, categoryId);
        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isCreated());

        UUID menuId = menuRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(menuTitle))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(bearer(delete("/admin/menus/{id}", menuId), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/menus/{id}", menuId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentMenu() throws Exception {
        MenuRequestDTO request = buildMenuRequest(menuTitle, categoryId);

        mockMvc.perform(bearer(put("/admin/menus/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)), adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenCreatingMenuWithMissingTitle() throws Exception {
        String invalidJson = String.format(
                "{\"title\":\"\",\"description\":\"Desc\",\"categoryId\":\"%s\"}", categoryId);

        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson), adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCreatingMenuWithoutCategory() throws Exception {
        String invalidJson = "{\"title\":\"Sem Cat\",\"description\":\"Desc\"}";

        mockMvc.perform(bearer(post("/admin/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson), adminToken))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── HELPERS ──────────────────────────────────

    private MenuRequestDTO buildMenuRequest(String title, UUID catId) {
        return new MenuRequestDTO(
                title, "Receita de integração",
                "https://img.test/menu.jpg", catId,
                "Modo de preparo: misturar tudo.",
                "Dica: consumir com água.",
                25.0, 40.0, 10.0, 5.0, 300.0);
    }
}
