package com.dvFabricio.VidaLongaFlix.categoryTest.controller;


import com.dvFabricio.VidaLongaFlix.controllers.CategoryController;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
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
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test
    void shouldReturnAllCategories() throws Exception {
        List<CategoryDTO> categories = List.of(new CategoryDTO(UUID.randomUUID(), "Health"), new CategoryDTO(UUID.randomUUID(), "Fitness"));

        when(categoryService.findAll()).thenReturn(categories);

        mockMvc.perform(get("/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(2)).andExpect(jsonPath("$[0].name").value("Health")).andExpect(jsonPath("$[1].name").value("Fitness"));

        verify(categoryService).findAll();
    }

    @Test
    void shouldReturnCategoryById() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryDTO categoryDTO = new CategoryDTO(categoryId, "Health");

        when(categoryService.findById(categoryId)).thenReturn(categoryDTO);

        mockMvc.perform(get("/categories/{id}", categoryId)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Health"));

        verify(categoryService).findById(categoryId);
    }

    @Test
    void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        UUID categoryId = UUID.randomUUID();

        when(categoryService.findById(categoryId)).thenThrow(new ResourceNotFoundExceptions("Category with ID " + categoryId + " not found."));

        mockMvc.perform(get("/categories/{id}", categoryId)).andExpect(status().isNotFound()).andExpect(content().string("Category with ID " + categoryId + " not found."));

        verify(categoryService).findById(categoryId);
    }

    @Test
    void shouldCreateCategory() throws Exception {
        CategoryDTO categoryDTO = new CategoryDTO(null, "Health");

        doNothing().when(categoryService).create(categoryDTO.name());

        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Health\"}")).andExpect(status().isCreated());

        verify(categoryService).create(categoryDTO.name());
    }

    @Test
    void shouldNotCreateDuplicateCategory() throws Exception {
        CategoryDTO categoryDTO = new CategoryDTO(null, "Health");

        doThrow(new DuplicateResourceException("name", "A category with the name 'Health' already exists."))
                .when(categoryService).create(categoryDTO.name());

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Health\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("name")) // Validar o campo "field"
                .andExpect(jsonPath("$.message").value("A category with the name 'Health' already exists.")); // Validar o campo "message"

        verify(categoryService).create(categoryDTO.name());
    }


    @Test
    void shouldUpdateCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryDTO categoryDTO = new CategoryDTO(categoryId, "Updated Health");

        doNothing().when(categoryService).update(categoryId, categoryDTO.name());

        mockMvc.perform(put("/categories/{id}", categoryId).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Updated Health\"}")).andExpect(status().isOk());

        verify(categoryService).update(categoryId, categoryDTO.name());
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();

        doNothing().when(categoryService).delete(categoryId);

        mockMvc.perform(delete("/categories/{id}", categoryId)).andExpect(status().isNoContent());

        verify(categoryService).delete(categoryId);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();

        doThrow(new ResourceNotFoundExceptions("Category with ID " + categoryId + " not found.")).when(categoryService).delete(categoryId);

        mockMvc.perform(delete("/categories/{id}", categoryId)).andExpect(status().isNotFound()).andExpect(content().string("Category with ID " + categoryId + " not found."));

        verify(categoryService).delete(categoryId);
    }
}

