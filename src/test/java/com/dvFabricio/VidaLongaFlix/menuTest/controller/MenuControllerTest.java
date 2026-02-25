package com.dvFabricio.VidaLongaFlix.menuTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.MenuController;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    private MockMvc mockMvc;

    @InjectMocks private MenuController menuController;
    @Mock private MenuService menuService;

    private UUID menuId;
    private UUID categoryId;
    private MenuDTO menuDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(menuController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        menuId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        Category category = new Category("Almoço", CategoryType.MENU);

        menuDTO = new MenuDTO(
                menuId,
                "Frango Grelhado",
                "Prato rico em proteína",
                "http://cover.com",
                category,
                "Grelhe por 20 min",
                "Prefira azeite",
                40.0, 10.0, 5.0, 2.0, 250.0
        );
    }

    @Test
    void shouldReturnAllMenus() throws Exception {
        when(menuService.findAll()).thenReturn(List.of(menuDTO));

        mockMvc.perform(get("/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].title").value("Frango Grelhado"));
    }

    @Test
    void shouldReturnMenuById() throws Exception {
        when(menuService.findById(menuId)).thenReturn(menuDTO);

        mockMvc.perform(get("/menus/{id}", menuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Frango Grelhado"))
                .andExpect(jsonPath("$.protein").value(40.0));
    }

    @Test
    void shouldReturnNotFoundWhenMenuDoesNotExist() throws Exception {
        when(menuService.findById(menuId))
                .thenThrow(new ResourceNotFoundExceptions(
                        "Menu com ID " + menuId + " não encontrado."));

        mockMvc.perform(get("/menus/{id}", menuId))
                .andExpect(status().isNotFound());
    }
}