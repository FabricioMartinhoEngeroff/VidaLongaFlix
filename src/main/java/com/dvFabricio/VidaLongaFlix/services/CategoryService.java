package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDTO> findAll() {
        try {
            return categoryRepository.findAll()
                    .stream()
                    .map(CategoryDTO::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new DatabaseException("Error while fetching all categories.");
        }
    }

    public CategoryDTO findById(UUID uuid) {
        return categoryRepository.findByUuid(uuid)
                .map(CategoryDTO::new)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Category with UUID " + uuid + " not found."));
    }

    @Transactional
    public CategoryDTO create(String name) {
        validateCategoryName(name);

        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("name", "A category with the name '" + name + "' already exists.");
        }

        try {
            Category category = new Category(name);
            category = categoryRepository.save(category);
            return new CategoryDTO(category);
        } catch (Exception e) {
            throw new DatabaseException("Error while creating category '" + name + "': " + e.getMessage());
        }
    }

    @Transactional
    public CategoryDTO update(UUID uuid, String name) {
        validateCategoryName(name);

        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Category with UUID " + uuid + " not found."));

        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("name", "A category with the name '" + name + "' already exists.");
        }

        try {
            category.setName(name);
            return new CategoryDTO(categoryRepository.save(category));
        } catch (Exception e) {
            throw new DatabaseException("Error while updating category with UUID " + uuid + ": " + e.getMessage());
        }
    }

    @Transactional
    public void delete(UUID uuid) {
        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Category with UUID " + uuid + " not found."));

        try {
            categoryRepository.delete(category);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting category with UUID " + uuid + ".");
        }
    }

    private void validateCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new MissingRequiredFieldException("name", "The category name is required.");
        }
    }
}

