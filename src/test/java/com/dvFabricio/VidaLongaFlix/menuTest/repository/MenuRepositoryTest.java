package com.dvFabricio.VidaLongaFlix.menuTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class MenuRepositoryTest {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;
    private Menu menu1;
    private Menu menu2;

    @BeforeEach
    void setup() {
        menuRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category("Almoço", CategoryType.MENU);
        categoryRepository.saveAndFlush(category);

        menu1 = Menu.builder()
                .title("Frango Grelhado")
                .description("Prato rico em proteína")
                .cover("http://cover1.com")
                .category(category)
                .recipe("Grelhe por 20 min")
                .nutritionistTips("Prefira azeite")
                .protein(40.0)
                .carbs(10.0)
                .fat(5.0)
                .fiber(2.0)
                .calories(250.0)
                .build();

        menu2 = Menu.builder()
                .title("Salada Caesar")
                .description("Leve e nutritiva")
                .cover("http://cover2.com")
                .category(category)
                .recipe("Monte a salada fria")
                .nutritionistTips("Evite excesso de molho")
                .protein(15.0)
                .carbs(8.0)
                .fat(12.0)
                .fiber(3.0)
                .calories(180.0)
                .build();

        menuRepository.saveAllAndFlush(List.of(menu1, menu2));
    }

    @Test
    void shouldSaveAndFindMenuById() {
        Optional<Menu> result = menuRepository.findById(menu1.getId());

        assertTrue(result.isPresent());
        assertEquals("Frango Grelhado", result.get().getTitle());
        assertEquals(40.0, result.get().getProtein());
    }

    @Test
    void shouldFindAllMenus() {
        List<Menu> result = menuRepository.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void shouldDeleteMenu() {
        menuRepository.delete(menu1);

        Optional<Menu> result = menuRepository.findById(menu1.getId());
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenMenuNotFound() {
        Optional<Menu> result = menuRepository.findById(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    void shouldUpdateMenuTitle() {
        menu1.setTitle("Frango ao Limão");
        menuRepository.saveAndFlush(menu1);

        Optional<Menu> updated = menuRepository.findById(menu1.getId());

        assertTrue(updated.isPresent());
        assertEquals("Frango ao Limão", updated.get().getTitle());
    }

    @Test
    void shouldPersistNutritionalData() {
        Optional<Menu> result = menuRepository.findById(menu2.getId());

        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(15.0, result.get().getProtein()),
                () -> assertEquals(8.0, result.get().getCarbs()),
                () -> assertEquals(12.0, result.get().getFat()),
                () -> assertEquals(3.0, result.get().getFiber()),
                () -> assertEquals(180.0, result.get().getCalories())
        );
    }

    @Test
    void shouldPersistCategoryRelationship() {
        Optional<Menu> result = menuRepository.findById(menu1.getId());

        assertTrue(result.isPresent());
        assertNotNull(result.get().getCategory());
        assertEquals("Almoço", result.get().getCategory().getName());
    }
}