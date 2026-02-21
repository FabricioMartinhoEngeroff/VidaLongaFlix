package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategorySummaryDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.Category;
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

    public List<CategorySummaryDTO> findAllSummary() {
        return categoryRepository.findAll().stream()
                .map(CategorySummaryDTO::new)
                .toList();
    }

    public CategorySummaryDTO findSummaryById(UUID id) {
        return new CategorySummaryDTO(findCategoryById(id));
    }

    @Transactional
    public void create(String name) {
        validateCategoryName(name);

        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "name",
                    "A category with this name already exists."
            );
        }

        Category category = new Category(name);
        saveCategory(category);
    }

    @Transactional
    public void update(UUID id, String name) {
        validateCategoryName(name);

        Category category = findCategoryById(id);

        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "name",
                    "A category with this name already exists."
            );
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
                .orElseThrow(() -> new ResourceNotFoundExceptions("Category with ID " + id + " not found."));
    }


    private void validateCategoryName(String name) {
        if (isBlank(name)) {
            throw new MissingRequiredFieldException("name", "The category name is required.");
        }
    }

    private boolean isBlank(String field) {
        return field == null || field.isBlank();
    }
}
