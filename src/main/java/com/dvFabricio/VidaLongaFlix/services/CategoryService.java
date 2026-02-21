package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDTO> findAllSummary(CategoryType type) {
        return categoryRepository.findByType(type).stream()
                .map(CategoryDTO::new)
                .toList();
    }


    public void create(String name, CategoryType type) {
        validateCategoryName(name);

        if (categoryRepository.existsByNameAndType(name, type)) {
            throw new DuplicateResourceException("name", "A category with this name already exists.");
        }

        saveCategory(new Category(name, type));
    }

    public void update(UUID id, String name) {
        Category category = findCategoryById(id);

        if (!category.getName().equalsIgnoreCase(name) &&
                categoryRepository.existsByNameAndType(name, category.getType())) {
            throw new DuplicateResourceException("name", "A category with this name already exists.");
        }

        category.setName(name);
        saveCategory(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = findCategoryById(id);
        try {
            categoryRepository.delete(category);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting category with ID " + id + ".");
        }
    }

    private void saveCategory(Category category) {
        try {
            categoryRepository.save(category);
        } catch (Exception e) {
            throw new DatabaseException("Error while saving category: " + e.getMessage());
        }
    }

    private Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Category with ID " + id + " not found."));
    }

    private void validateCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new MissingRequiredFieldException("name", "The category name is required.");
        }
    }
}