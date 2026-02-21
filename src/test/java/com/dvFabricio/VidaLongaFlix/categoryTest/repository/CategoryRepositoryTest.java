package com.dvFabricio.VidaLongaFlix.categoryTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setup() {
        category = new Category("Health", CategoryType.VIDEO);
        categoryRepository.save(category);
    }

    @Test
    void shouldFindCategoryById() {
        Optional<Category> result = categoryRepository.findById(category.getId());
        assertTrue(result.isPresent());
        assertEquals("Health", result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<Category> result = categoryRepository.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindByType() {
        categoryRepository.save(new Category("LowCarb", CategoryType.MENU));

        List<Category> videos = categoryRepository.findByType(CategoryType.VIDEO);
        List<Category> menus = categoryRepository.findByType(CategoryType.MENU);

        assertEquals(1, videos.size());
        assertEquals(1, menus.size());
    }

    @Test
    void shouldCheckExistsByNameAndType() {
        assertTrue(categoryRepository.existsByNameAndType("Health", CategoryType.VIDEO));
        assertFalse(categoryRepository.existsByNameAndType("Health", CategoryType.MENU));
        assertFalse(categoryRepository.existsByNameAndType("Fitness", CategoryType.VIDEO));
    }

    @Test
    void shouldDeleteCategory() {
        categoryRepository.delete(category);
        assertTrue(categoryRepository.findById(category.getId()).isEmpty());
    }
}