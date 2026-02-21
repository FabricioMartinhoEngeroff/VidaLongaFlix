package com.dvFabricio.VidaLongaFlix.categoryTest.service;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
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
        category = new Category("Health", CategoryType.VIDEO);
        category.setId(UUID.randomUUID());
    }

    @Test
    void shouldFindAllByType() {
        given(categoryRepository.findByType(CategoryType.VIDEO))
                .willReturn(List.of(category));

        List<CategoryDTO> result = categoryService.findAllSummary(CategoryType.VIDEO);

        assertEquals(1, result.size());
        assertEquals("Health", result.get(0).name());
        then(categoryRepository).should().findByType(CategoryType.VIDEO);
    }

    @Test
    void shouldCreateCategory() {
        given(categoryRepository.existsByNameAndType("Health", CategoryType.VIDEO))
                .willReturn(false);

        assertDoesNotThrow(() -> categoryService.create("Health", CategoryType.VIDEO));

        then(categoryRepository).should().save(any(Category.class));
    }

    @Test
    void shouldNotCreateDuplicateCategory() {
        given(categoryRepository.existsByNameAndType("Health", CategoryType.VIDEO))
                .willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> categoryService.create("Health", CategoryType.VIDEO));

        then(categoryRepository).should(never()).save(any(Category.class));
    }

    @Test
    void shouldUpdateCategory() {
        given(categoryRepository.findById(category.getId()))
                .willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndType("Updated", CategoryType.VIDEO))
                .willReturn(false);

        assertDoesNotThrow(() -> categoryService.update(category.getId(), "Updated"));

        then(categoryRepository).should().save(any(Category.class));
    }

    @Test
    void shouldNotUpdateWithDuplicateName() {
        given(categoryRepository.findById(category.getId()))
                .willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndType("Fitness", CategoryType.VIDEO))
                .willReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> categoryService.update(category.getId(), "Fitness"));
    }

    @Test
    void shouldDeleteCategory() {
        given(categoryRepository.findById(category.getId()))
                .willReturn(Optional.of(category));

        assertDoesNotThrow(() -> categoryService.delete(category.getId()));

        then(categoryRepository).should().delete(category);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> categoryService.delete(id));
    }
}