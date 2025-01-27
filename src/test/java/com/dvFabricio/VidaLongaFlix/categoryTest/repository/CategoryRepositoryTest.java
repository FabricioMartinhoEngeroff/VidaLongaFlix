package com.dvFabricio.VidaLongaFlix.categoryTest.repository;


import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setup() {
        category = new Category("Health");
        categoryRepository.save(category);
    }

    @Test
    void shouldFindCategoryById() {
        Optional<Category> result = categoryRepository.findById(category.getId());

        assertTrue(result.isPresent(), "Category should be found by ID.");
        assertEquals("Health", result.get().getName(), "Category name should match.");
    }

    @Test
    void shouldReturnEmptyWhenCategoryNotFoundById() {
        UUID nonExistentId = UUID.randomUUID();

        Optional<Category> result = categoryRepository.findById(nonExistentId);

        assertTrue(result.isEmpty(), "No category should be found for non-existent ID.");
    }

    @Test
    void shouldCheckIfCategoryExistsByName() {
        boolean exists = categoryRepository.existsByName("Health");

        assertTrue(exists, "Category with name 'Health' should exist.");
    }

    @Test
    void shouldReturnFalseIfCategoryDoesNotExistByName() {
        boolean exists = categoryRepository.existsByName("Fitness");

        assertFalse(exists, "Category with name 'Fitness' should not exist.");
    }

    @Test
    void shouldSaveCategory() {
        Category newCategory = new Category("Fitness");
        Category savedCategory = categoryRepository.save(newCategory);

        assertNotNull(savedCategory.getId(), "Saved category should have an ID.");
        assertEquals("Fitness", savedCategory.getName(), "Saved category name should match.");
    }

    @Test
    void shouldDeleteCategory() {
        categoryRepository.delete(category);

        Optional<Category> result = categoryRepository.findById(category.getId());

        assertTrue(result.isEmpty(), "Category should be deleted and not found.");
    }
}
