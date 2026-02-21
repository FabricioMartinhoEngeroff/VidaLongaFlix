package com.dvFabricio.VidaLongaFlix.controllers;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryRequestDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> findAllSummary(@RequestParam CategoryType type) {
        return ResponseEntity.ok(categoryService.findAllSummary(type));
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid CategoryRequestDTO request) {
        categoryService.create(request.name(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @RequestBody @Valid CategoryRequestDTO request) {
        categoryService.update(id, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}