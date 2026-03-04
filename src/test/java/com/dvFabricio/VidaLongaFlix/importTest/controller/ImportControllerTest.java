package com.dvFabricio.VidaLongaFlix.importTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.ImportController;
import com.dvFabricio.VidaLongaFlix.domain.imports.ImportResultDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.services.ImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ImportController importController;

    @Mock
    private ImportService importService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(importController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─────────────────────── IMPORT VIDEOS ───────────────────────

    @Test
    void shouldImportVideosAndReturnOk() throws Exception {
        when(importService.importVideos(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(2, 0, List.of()));

        mockMvc.perform(multipart("/admin/import/videos")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    void shouldReturnImportedCountForVideos() throws Exception {
        when(importService.importVideos(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(5, 0, List.of()));

        mockMvc.perform(multipart("/admin/import/videos")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(5));
    }

    @Test
    void shouldReturnErrorsWhenVideoRowsAreInvalid() throws Exception {
        List<String> errors = List.of(
                "Linha 2: 'title' é obrigatório",
                "Linha 3: categoria 'Inexistente' não encontrada (tipo VIDEO)."
        );
        when(importService.importVideos(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(0, 2, errors));

        mockMvc.perform(multipart("/admin/import/videos")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldReturnPartialResultWhenSomeVideoRowsFail() throws Exception {
        List<String> errors = List.of("Linha 3: 'url' é obrigatório");
        when(importService.importVideos(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(2, 1, errors));

        mockMvc.perform(multipart("/admin/import/videos")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    // ─────────────────────── IMPORT MENUS ────────────────────────

    @Test
    void shouldImportMenusAndReturnOk() throws Exception {
        when(importService.importMenus(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(3, 0, List.of()));

        mockMvc.perform(multipart("/admin/import/menus")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(3))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldReturnErrorsWhenMenuRowsAreInvalid() throws Exception {
        List<String> errors = List.of("Linha 2: categoria 'Jantar' não encontrada (tipo MENU).");
        when(importService.importMenus(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(0, 1, errors));

        mockMvc.perform(multipart("/admin/import/menus")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1));
    }

    @Test
    void shouldReturnEmptyErrorsWhenAllMenusImported() throws Exception {
        when(importService.importMenus(any(MultipartFile.class)))
                .thenReturn(new ImportResultDTO(4, 0, List.of()));

        mockMvc.perform(multipart("/admin/import/menus")
                        .file(validCsvFile("file"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(4))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    // ─────────────────────── HELPER ──────────────────────────────

    private MockMultipartFile validCsvFile(String paramName) {
        String content = "title,description,url,cover,categoryName\nTest,Desc,http://url,http://cover,Saúde";
        return new MockMultipartFile(
                paramName, "data.csv", MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8));
    }
}
