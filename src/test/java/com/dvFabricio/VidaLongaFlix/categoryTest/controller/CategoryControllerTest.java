package com.dvFabricio.VidaLongaFlix.categoryTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.CategoryController;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CategoryControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private CategoryController categoryController;

    @Mock
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnAllCategoriesByType() throws Exception {
        List<CategoryDTO> categories = List.of(
                new CategoryDTO(UUID.randomUUID(), "Health", CategoryType.VIDEO)
        );

        when(categoryService.findAllSummary(CategoryType.VIDEO)).thenReturn(categories);

        mockMvc.perform(get("/categories").param("type", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].name").value("Health"));
    }

    @Test
    void shouldCreateCategory() throws Exception {
        doNothing().when(categoryService).create("Health", CategoryType.VIDEO);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Health\",\"type\":\"VIDEO\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldNotCreateDuplicate() throws Exception {
        doThrow(new DuplicateResourceException("name", "A category with this name already exists."))
                .when(categoryService).create("Health", CategoryType.VIDEO);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Health\",\"type\":\"VIDEO\"}"))
                .andExpect(status().isConflict()); // ← 409 em vez de 400
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(categoryService).delete(id);

        mockMvc.perform(delete("/categories/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistent() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundExceptions("Category with ID " + id + " not found."))
                .when(categoryService).delete(id);

        mockMvc.perform(delete("/categories/{id}", id))
                .andExpect(status().isNotFound());
    }
}