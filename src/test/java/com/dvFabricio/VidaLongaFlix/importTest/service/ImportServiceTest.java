package com.dvFabricio.VidaLongaFlix.importTest.service;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.imports.ImportResultDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import com.dvFabricio.VidaLongaFlix.services.ImportService;
import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @InjectMocks
    private ImportService importService;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private Category videoCategory;
    private Category menuCategory;

    @BeforeEach
    void setup() {
        videoCategory = new Category("Saúde", CategoryType.VIDEO);
        videoCategory.setId(UUID.randomUUID());

        menuCategory = new Category("Almoço", CategoryType.MENU);
        menuCategory.setId(UUID.randomUUID());
    }

    // ─────────────────────── IMPORT VIDEOS ───────────────────────

    @Test
    void shouldImportVideoSuccessfully() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Yoga Matinal,Alongamento para iniciantes,https://url.com,https://cover.com,Saúde";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Saúde", CategoryType.VIDEO))
                .willReturn(Optional.of(videoCategory));
        given(videoRepository.save(any(Video.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
        then(videoRepository).should().save(any(Video.class));
    }

    @Test
    void shouldImportMultipleVideos() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Video 1,Desc 1,https://url1.com,https://cover1.com,Saúde\n" +
                "Video 2,Desc 2,https://url2.com,https://cover2.com,Saúde\n" +
                "Video 3,Desc 3,https://url3.com,https://cover3.com,Saúde";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Saúde", CategoryType.VIDEO))
                .willReturn(Optional.of(videoCategory));
        given(videoRepository.save(any(Video.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(3, result.imported());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldSkipVideoWithMissingTitle() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                ",Descrição válida,https://url.com,https://cover.com,Saúde";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("title"));
        then(videoRepository).should(never()).save(any(Video.class));
    }

    @Test
    void shouldSkipVideoWithMissingDescription() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Yoga Matinal,,https://url.com,https://cover.com,Saúde";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("description"));
    }

    @Test
    void shouldSkipVideoWithMissingUrl() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Yoga Matinal,Descrição,,https://cover.com,Saúde";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("url"));
    }

    @Test
    void shouldSkipVideoWithMissingCategoryName() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Yoga Matinal,Descrição,https://url.com,https://cover.com,";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("categoryName"));
    }

    @Test
    void shouldSkipVideoWhenCategoryNotFound() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Yoga Matinal,Descrição,https://url.com,https://cover.com,CategoriaInexistente";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("CategoriaInexistente", CategoryType.VIDEO))
                .willReturn(Optional.empty());

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("CategoriaInexistente"));
        then(videoRepository).should(never()).save(any(Video.class));
    }

    @Test
    void shouldImportVideosWithPartialErrors() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Video Válido,Desc,https://url.com,https://cover.com,Saúde\n" +
                ",Sem título,https://url2.com,https://cover2.com,Saúde\n" +
                "Outro Válido,Desc2,https://url3.com,https://cover3.com,Saúde";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Saúde", CategoryType.VIDEO))
                .willReturn(Optional.of(videoCategory));
        given(videoRepository.save(any(Video.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(2, result.imported());
        assertEquals(1, result.skipped());
        assertEquals(1, result.errors().size());
    }

    @Test
    void shouldImportVideoWithOptionalNutritionalFields() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName,recipe,protein,carbs,fat,fiber,calories\n" +
                "Yoga,Desc,https://url.com,https://cover.com,Saúde,Receita aqui,30.5,10.0,5.0,2.0,200.0";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Saúde", CategoryType.VIDEO))
                .willReturn(Optional.of(videoCategory));
        given(videoRepository.save(any(Video.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());
    }

    @Test
    void shouldImportVideoWithoutOptionalFields() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n" +
                "Yoga,Desc,https://url.com,,Saúde";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Saúde", CategoryType.VIDEO))
                .willReturn(Optional.of(videoCategory));
        given(videoRepository.save(any(Video.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(1, result.imported());
    }

    // ─────────────────────── IMPORT MENUS ────────────────────────

    @Test
    void shouldImportMenuSuccessfully() throws IOException, CsvValidationException {
        String csv = "title,description,cover,categoryName,recipe,nutritionistTips,protein,carbs,fat,fiber,calories\n" +
                "Frango Grelhado,Prato rico em proteína,https://cover.com,Almoço,Grelhe 20min,Use azeite,40.0,10.0,5.0,2.0,250.0";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Almoço", CategoryType.MENU))
                .willReturn(Optional.of(menuCategory));
        given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importMenus(file);

        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
        then(menuRepository).should().save(any(Menu.class));
    }

    @Test
    void shouldSkipMenuWithMissingTitle() throws IOException, CsvValidationException {
        String csv = "title,description,cover,categoryName\n" +
                ",Descrição,https://cover.com,Almoço";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importMenus(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("title"));
        then(menuRepository).should(never()).save(any(Menu.class));
    }

    @Test
    void shouldSkipMenuWithMissingDescription() throws IOException, CsvValidationException {
        String csv = "title,description,cover,categoryName\n" +
                "Frango Grelhado,,https://cover.com,Almoço";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importMenus(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("description"));
    }

    @Test
    void shouldSkipMenuWhenCategoryNotFound() throws IOException, CsvValidationException {
        String csv = "title,description,cover,categoryName\n" +
                "Frango Grelhado,Desc,https://cover.com,CategoriaInexistente";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("CategoriaInexistente", CategoryType.MENU))
                .willReturn(Optional.empty());

        ImportResultDTO result = importService.importMenus(file);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        assertTrue(result.errors().get(0).contains("CategoriaInexistente"));
        then(menuRepository).should(never()).save(any(Menu.class));
    }

    @Test
    void shouldImportMenusWithPartialErrors() throws IOException, CsvValidationException {
        String csv = "title,description,cover,categoryName\n" +
                "Frango,Desc 1,https://cover.com,Almoço\n" +
                ",Sem título,https://cover.com,Almoço\n" +
                "Salada,Desc 2,https://cover.com,Almoço";
        MockMultipartFile file = csvFile(csv);

        given(categoryRepository.findByNameAndType("Almoço", CategoryType.MENU))
                .willReturn(Optional.of(menuCategory));
        given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

        ImportResultDTO result = importService.importMenus(file);

        assertEquals(2, result.imported());
        assertEquals(1, result.skipped());
    }

    @Test
    void shouldReturnEmptyResultForEmptyCsvVideos() throws IOException, CsvValidationException {
        String csv = "title,description,url,cover,categoryName\n";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importVideos(file);

        assertEquals(0, result.imported());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldReturnEmptyResultForEmptyCsvMenus() throws IOException, CsvValidationException {
        String csv = "title,description,cover,categoryName\n";
        MockMultipartFile file = csvFile(csv);

        ImportResultDTO result = importService.importMenus(file);

        assertEquals(0, result.imported());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
    }

    // ─────────────────────── HELPER ──────────────────────────────

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file", "data.csv", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
