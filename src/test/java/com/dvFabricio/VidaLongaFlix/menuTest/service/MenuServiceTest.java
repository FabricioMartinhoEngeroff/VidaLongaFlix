package com.dvFabricio.VidaLongaFlix.menuTest.service;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import com.dvFabricio.VidaLongaFlix.services.MenuService;
import com.dvFabricio.VidaLongaFlix.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @InjectMocks private MenuService menuService;
    @Mock private MenuRepository menuRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private NotificationService notificationService;

    private Menu menu;
    private Category category;
    private UUID menuId;
    private UUID categoryId;

    @BeforeEach
    void setup() {
        categoryId = UUID.randomUUID();
        category = new Category("Almoço", CategoryType.MENU);
        ReflectionTestUtils.setField(category, "id", categoryId);

        menu = Menu.builder()
                .title("Frango Grelhado")
                .description("Prato rico em proteína")
                .cover("http://cover.com")
                .category(category)
                .recipe("Grelhe o frango por 20 minutos")
                .nutritionistTips("Prefira azeite de oliva")
                .protein(40.0)
                .carbs(10.0)
                .fat(5.0)
                .fiber(2.0)
                .calories(250.0)
                .build();

        menuId = UUID.randomUUID();
        ReflectionTestUtils.setField(menu, "id", menuId);
    }

    @Test
    void shouldFindAll() {
        given(menuRepository.findAll()).willReturn(List.of(menu));

        List<MenuDTO> result = menuService.findAll();

        assertEquals(1, result.size());
        assertEquals("Frango Grelhado", result.get(0).title());
        then(menuRepository).should().findAll();
    }

    @Test
    void shouldFindById() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        MenuDTO result = menuService.findById(menuId);

        assertEquals("Frango Grelhado", result.title());
        assertEquals(40.0, result.protein());
    }

    @Test
    void shouldThrowWhenMenuNotFound() {
        given(menuRepository.findById(menuId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> menuService.findById(menuId));
    }

    @Test
    void shouldCreateMenu() {
        MenuRequestDTO request = new MenuRequestDTO(
                "Frango Grelhado", "Prato rico em proteína",
                "http://cover.com", categoryId,
                "Grelhe por 20 min", "Prefira azeite",
                40.0, 10.0, 5.0, 2.0, 250.0);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        assertDoesNotThrow(() -> menuService.create(request));
        then(menuRepository).should().save(any(Menu.class));
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnCreate() {
        MenuRequestDTO request = new MenuRequestDTO(
                "Frango Grelhado", "Descrição",
                null, categoryId,
                null, null,
                null, null, null, null, null);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> menuService.create(request));
        then(menuRepository).should(never()).save(any());
    }

    @Test
    void shouldUpdateMenu() {
        MenuRequestDTO request = new MenuRequestDTO(
                "Frango ao Limão", "Versão atualizada",
                null, null,
                null, null,
                45.0, null, null, null, null);

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        assertDoesNotThrow(() -> menuService.update(menuId, request));

        assertEquals("Frango ao Limão", menu.getTitle());
        assertEquals(45.0, menu.getProtein());
        then(menuRepository).should().save(menu);
    }

    @Test
    void shouldNotUpdateFieldsWhenNullOrBlank() {
        // Request com campos nulos — nenhum campo deve ser alterado
        MenuRequestDTO request = new MenuRequestDTO(
                null, null, null, null,
                null, null,
                null, null, null, null, null);

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        menuService.update(menuId, request);

        assertEquals("Frango Grelhado", menu.getTitle());
        assertEquals(40.0, menu.getProtein());
    }

    @Test
    void shouldDeleteMenu() {
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        assertDoesNotThrow(() -> menuService.delete(menuId));
        then(menuRepository).should().delete(menu);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentMenu() {
        given(menuRepository.findById(menuId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> menuService.delete(menuId));
        then(menuRepository).should(never()).delete(any());
    }
}