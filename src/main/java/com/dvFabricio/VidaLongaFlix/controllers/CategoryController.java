package com.dvFabricio.VidaLongaFlix.controllers;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategorySummaryDTO;
import com.dvFabricio.VidaLongaFlix.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategorySummaryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllSummary());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorySummaryDTO> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.findSummaryById(id));
    }

    @PostMapping
    public ResponseEntity<Void> createCategory(@Valid @RequestBody CategoryRequestDTO request) {
        categoryService.create(request.name());
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequestDTO request) {
        categoryService.update(id, request.name());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}