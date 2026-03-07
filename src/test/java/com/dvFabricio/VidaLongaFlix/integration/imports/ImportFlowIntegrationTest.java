package com.dvFabricio.VidaLongaFlix.integration.imports;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.integration.base.BaseIntegrationTest;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para importação de vídeos e menus via CSV.
 * Sobe contexto Spring completo com H2 em memória.
 */
class ImportFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private MenuRepository menuRepository;

    private String adminToken;
    private String videoCategoryName;
    private String menuCategoryName;
    private UUID videoCategoryId;
    private UUID menuCategoryId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();

        videoCategoryName = "ImportVideo-" + UUID.randomUUID();
        menuCategoryName  = "ImportMenu-"  + UUID.randomUUID();

        Category videoCategory = categoryRepository.save(
                new Category(videoCategoryName, CategoryType.VIDEO));
        videoCategoryId = videoCategory.getId();

        Category menuCategory = categoryRepository.save(
                new Category(menuCategoryName, CategoryType.MENU));
        menuCategoryId = menuCategory.getId();
    }

    @AfterEach
    void cleanup() {
        videoRepository.findAll().stream()
                .filter(v -> v.getTitle().startsWith("Import-IT-"))
                .forEach(videoRepository::delete);

        menuRepository.findAll().stream()
                .filter(m -> m.getTitle().startsWith("Import-IT-"))
                .forEach(menuRepository::delete);

        categoryRepository.findAll().stream()
                .filter(c -> c.getId().equals(videoCategoryId)
                        || c.getId().equals(menuCategoryId)
                        || c.getName().startsWith("AutoCreate-"))
                .forEach(categoryRepository::delete);
    }

    // ─────────────────────── IMPORT VIDEOS ───────────────────────

    @Test
    void shouldImportVideosWithValidCsv() throws Exception {
        String csv = "title,description,url,cover,categoryName\n" +
                "Import-IT-Video1,Descrição 1,https://url1.com,https://cover1.com," + videoCategoryName + "\n" +
                "Import-IT-Video2,Descrição 2,https://url2.com,https://cover2.com," + videoCategoryName;

        mockMvc.perform(bearer(
                        multipart("/admin/import/videos").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    void shouldPersistImportedVideosInDatabase() throws Exception {
        String title = "Import-IT-Persistido-" + UUID.randomUUID();
        String csv = "title,description,url,cover,categoryName\n" +
                title + ",Desc,https://url.com,https://cover.com," + videoCategoryName;

        mockMvc.perform(bearer(
                        multipart("/admin/import/videos").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        boolean exists = videoRepository.findAll().stream()
                .anyMatch(v -> v.getTitle().equals(title));
        org.junit.jupiter.api.Assertions.assertTrue(exists);
    }

    @Test
    void shouldSkipVideoRowWithMissingRequiredField() throws Exception {
        String csv = "title,description,url,cover,categoryName\n" +
                ",Descrição sem título,https://url.com,https://cover.com," + videoCategoryName;

        mockMvc.perform(bearer(
                        multipart("/admin/import/videos").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void shouldAutoCreateCategoryAndImportVideoWhenCategoryDoesNotExist() throws Exception {
        String newCategory = "AutoCreate-Video-" + UUID.randomUUID();
        String csv = "title,description,url,cover,categoryName\n" +
                "Import-IT-Video,Desc,https://url.com,https://cover.com," + newCategory;

        mockMvc.perform(bearer(
                        multipart("/admin/import/videos").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    void shouldImportVideosWithPartialErrors() throws Exception {
        String csv = "title,description,url,cover,categoryName\n" +
                "Import-IT-Válido,Desc,https://url.com,https://cover.com," + videoCategoryName + "\n" +
                ",Sem título,https://url2.com,https://cover2.com," + videoCategoryName;

        mockMvc.perform(bearer(
                        multipart("/admin/import/videos").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void shouldReturn403WhenImportingVideosWithoutToken() throws Exception {
        mockMvc.perform(multipart("/admin/import/videos")
                        .file(csvMultipart("title,description,url,cover,categoryName\n")))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── IMPORT MENUS ────────────────────────

    @Test
    void shouldImportMenusWithValidCsv() throws Exception {
        String csv = "title,description,cover,categoryName,recipe,nutritionistTips,protein,carbs,fat,fiber,calories\n" +
                "Import-IT-Menu1,Prato 1,https://cover.com," + menuCategoryName + ",Receita,Dicas,40,10,5,2,250\n" +
                "Import-IT-Menu2,Prato 2,https://cover.com," + menuCategoryName + ",,,,,,,";

        mockMvc.perform(bearer(
                        multipart("/admin/import/menus").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    void shouldPersistImportedMenusInDatabase() throws Exception {
        String title = "Import-IT-Menu-Persistido-" + UUID.randomUUID();
        String csv = "title,description,cover,categoryName\n" +
                title + ",Desc,https://cover.com," + menuCategoryName;

        mockMvc.perform(bearer(
                        multipart("/admin/import/menus").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        boolean exists = menuRepository.findAll().stream()
                .anyMatch(m -> m.getTitle().equals(title));
        org.junit.jupiter.api.Assertions.assertTrue(exists);
    }

    @Test
    void shouldAutoCreateCategoryAndImportMenuWhenCategoryDoesNotExist() throws Exception {
        String newCategory = "AutoCreate-Menu-" + UUID.randomUUID();
        String csv = "title,description,cover,categoryName\n" +
                "Import-IT-Menu,Desc,https://cover.com," + newCategory;

        mockMvc.perform(bearer(
                        multipart("/admin/import/menus").file(csvMultipart(csv)),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    void shouldReturn403WhenImportingMenusWithoutToken() throws Exception {
        mockMvc.perform(multipart("/admin/import/menus")
                        .file(csvMultipart("title,description,cover,categoryName\n")))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── HELPER ──────────────────────────────

    private MockMultipartFile csvMultipart(String content) {
        return new MockMultipartFile(
                "file", "data.csv", MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8));
    }
}
