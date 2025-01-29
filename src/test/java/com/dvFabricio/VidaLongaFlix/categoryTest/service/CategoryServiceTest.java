package com.dvFabricio.VidaLongaFlix.categoryTest.service;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.Category;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.services.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setup() {
        category = new Category("Health");
        category.setId(UUID.randomUUID());
    }

    @Test
    void shouldFindAllCategories() {
        Category anotherCategory = new Category("Fitness");
        given(categoryRepository.findAll()).willReturn(List.of(category, anotherCategory));

        List<CategoryDTO> result = categoryService.findAll();

        assertAll(
                () -> assertEquals(2, result.size(), "Should return 2 categories"),
                () -> assertEquals("Health", result.get(0).name(), "First category name should be 'Health'"),
                () -> assertEquals("Fitness", result.get(1).name(), "Second category name should be 'Fitness'")
        );

        then(categoryRepository).should().findAll();
    }

    @Test
    void shouldFindCategoryById() {
        UUID categoryId = category.getId();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        CategoryDTO result = categoryService.findById(categoryId);

        assertAll(
                () -> assertNotNull(result, "Result should not be null"),
                () -> assertEquals("Health", result.name(), "Category name should be 'Health'")
        );

        then(categoryRepository).should().findById(categoryId);
    }

    @Test
    void shouldThrowExceptionWhenCategoryNotFoundById() {
        UUID categoryId = UUID.randomUUID();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        ResourceNotFoundExceptions exception = assertThrows(
                ResourceNotFoundExceptions.class,
                () -> categoryService.findById(categoryId)
        );

        assertEquals("Category with ID " + categoryId + " not found.", exception.getMessage());
        then(categoryRepository).should().findById(categoryId);
    }

    @Test
    void shouldCreateCategory() {
        given(categoryRepository.existsByName(category.getName())).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willReturn(category);

        assertDoesNotThrow(() -> categoryService.create(category.getName()));

        then(categoryRepository).should().existsByName(category.getName());
        then(categoryRepository).should().save(any(Category.class));
    }

    @Test
    void shouldNotCreateDuplicateCategory() {
        given(categoryRepository.existsByName(category.getName())).willReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> categoryService.create(category.getName())
        );

        assertEquals("A category with this name already exists.", exception.getMessage());
        then(categoryRepository).should().existsByName(category.getName());
        then(categoryRepository).should(never()).save(any(Category.class));
    }


    @Test
    void shouldUpdateCategory() {
        UUID categoryId = category.getId();
        String updatedName = "Updated Health";

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByName(updatedName)).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willReturn(category);

        assertDoesNotThrow(() -> categoryService.update(categoryId, updatedName));

        then(categoryRepository).should().findById(categoryId);
        then(categoryRepository).should().existsByName(updatedName);
        then(categoryRepository).should().save(any(Category.class));
    }

    @Test
    void shouldNotUpdateCategoryWithDuplicateName() {
        UUID categoryId = category.getId();
        String duplicateName = "Duplicate Health";

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByName(duplicateName)).willReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> categoryService.update(categoryId, duplicateName)
        );

        assertEquals("A category with this name already exists.", exception.getMessage());
        then(categoryRepository).should().findById(categoryId);
        then(categoryRepository).should().existsByName(duplicateName);
        then(categoryRepository).should(never()).save(any(Category.class));
    }

    @Test
    void shouldDeleteCategory() {
        UUID categoryId = category.getId();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        assertDoesNotThrow(() -> categoryService.delete(categoryId));

        then(categoryRepository).should().findById(categoryId);
        then(categoryRepository).should().delete(category);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCategory() {
        UUID categoryId = UUID.randomUUID();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        ResourceNotFoundExceptions exception = assertThrows(
                ResourceNotFoundExceptions.class,
                () -> categoryService.delete(categoryId)
        );

        assertEquals("Category with ID " + categoryId + " not found.", exception.getMessage());
        then(categoryRepository).should().findById(categoryId);
        then(categoryRepository).should(never()).delete(any(Category.class));
    }
}

