package com.dvFabricio.VidaLongaFlix.menuTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuTest {

    private Category category;
    private Menu menu;

    @BeforeEach
    void setup() {
        category = new Category("Almoço", CategoryType.MENU);

        menu = Menu.builder()
                .title("Frango Grelhado")
                .description("Prato rico em proteína")
                .cover("http://cover.com")
                .category(category)
                .recipe("Grelhe por 20 min")
                .nutritionistTips("Prefira azeite")
                .protein(40.0)
                .carbs(10.0)
                .fat(5.0)
                .fiber(2.0)
                .calories(250.0)
                .build();
    }

    @Test
    void shouldBuildMenuCorrectly() {
        assertAll(
                () -> assertEquals("Frango Grelhado", menu.getTitle()),
                () -> assertEquals("Prato rico em proteína", menu.getDescription()),
                () -> assertEquals("http://cover.com", menu.getCover()),
                () -> assertEquals(40.0, menu.getProtein()),
                () -> assertEquals(10.0, menu.getCarbs()),
                () -> assertEquals(5.0, menu.getFat()),
                () -> assertEquals(2.0, menu.getFiber()),
                () -> assertEquals(250.0, menu.getCalories())
        );
    }

    @Test
    void shouldMapMenuToDTO() {
        MenuDTO dto = new MenuDTO(menu);

        assertAll(
                () -> assertEquals(menu.getTitle(), dto.title()),
                () -> assertEquals(menu.getDescription(), dto.description()),
                () -> assertEquals(menu.getCover(), dto.cover()),
                () -> assertEquals(menu.getProtein(), dto.protein()),
                () -> assertEquals(menu.getCalories(), dto.calories()),
                () -> assertEquals(menu.getCategory(), dto.category())
        );
    }

    @Test
    void shouldCreateMenuRequestDTO() {
        MenuRequestDTO request = new MenuRequestDTO(
                "Frango Grelhado", "Prato rico em proteína",
                "http://cover.com", null,
                "Grelhe por 20 min", "Prefira azeite",
                40.0, 10.0, 5.0, 2.0, 250.0);

        assertAll(
                () -> assertEquals("Frango Grelhado", request.title()),
                () -> assertEquals(40.0, request.protein()),
                () -> assertEquals(250.0, request.calories())
        );
    }

    @Test
    void shouldUpdateMenuFieldsIndependently() {
        menu.setTitle("Frango ao Limão");
        menu.setProtein(45.0);

        assertEquals("Frango ao Limão", menu.getTitle());
        assertEquals(45.0, menu.getProtein());
        assertEquals("Prato rico em proteína", menu.getDescription()); // <- rico
        assertEquals(10.0, menu.getCarbs());
    }

    @Test
    void shouldHaveCategoryAssociated() {
        assertNotNull(menu.getCategory());
        assertEquals("Almoço", menu.getCategory().getName());
        assertEquals(CategoryType.MENU, menu.getCategory().getType());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        Menu minimal = Menu.builder()
                .title("Salada")
                .description("Simples e leve")
                .category(category)
                .build();

        assertAll(
                () -> assertNull(minimal.getCover()),
                () -> assertNull(minimal.getRecipe()),
                () -> assertNull(minimal.getNutritionistTips()),
                () -> assertNull(minimal.getProtein())
        );
    }
}