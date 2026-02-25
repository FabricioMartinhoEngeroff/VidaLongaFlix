package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.menu.MenuDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuRequestDTO;
import com.dvFabricio.VidaLongaFlix.services.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/menus")
public class AdminMenuController {

    private final MenuService menuService;

    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid MenuRequestDTO request) {
        menuService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuDTO> update(
            @PathVariable UUID id,
            @RequestBody @Valid MenuRequestDTO request) {
        menuService.update(id, request);
        return ResponseEntity.ok(menuService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}