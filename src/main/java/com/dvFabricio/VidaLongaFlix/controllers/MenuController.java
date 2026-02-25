package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.menu.MenuDTO;
import com.dvFabricio.VidaLongaFlix.services.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<List<MenuDTO>> findAll() {
        return ResponseEntity.ok(menuService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(menuService.findById(id));
    }
}