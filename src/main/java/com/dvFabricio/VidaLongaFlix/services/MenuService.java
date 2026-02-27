package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.MenuRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

    public MenuService(MenuRepository menuRepository, CategoryRepository categoryRepository) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
    }

    // Busca todos os menus — rota pública
    @Transactional(readOnly = true)
    public List<MenuDTO> findAll() {
        return menuRepository.findAll().stream()
                .map(MenuDTO::new)
                .toList();
    }

    // Busca um menu por ID — rota pública
    @Transactional(readOnly = true)
    public MenuDTO findById(UUID id) {
        return new MenuDTO(findMenuById(id));
    }

    // Cria um novo menu — só ADMIN
    @Transactional
    public void create(MenuRequestDTO request) {
        Menu menu = Menu.builder()
                .title(request.title())
                .description(request.description())
                .cover(request.cover())
                .category(findCategoryById(request.categoryId()))
                .recipe(request.recipe())
                .nutritionistTips(request.nutritionistTips())
                .protein(request.protein())
                .carbs(request.carbs())
                .fat(request.fat())
                .fiber(request.fiber())
                .calories(request.calories())
                .build();

        saveMenu(menu);
    }

    // Atualiza só os campos enviados — só ADMIN
    @Transactional
    public void update(UUID id, MenuRequestDTO request) {
        Menu menu = findMenuById(id);

        if (!isBlank(request.title()))       menu.setTitle(request.title());
        if (!isBlank(request.description())) menu.setDescription(request.description());
        if (!isBlank(request.cover()))       menu.setCover(request.cover());
        if (request.categoryId() != null)    menu.setCategory(findCategoryById(request.categoryId()));
        if (!isBlank(request.recipe()))      menu.setRecipe(request.recipe());
        if (!isBlank(request.nutritionistTips())) menu.setNutritionistTips(request.nutritionistTips());
        if (request.protein() != null)       menu.setProtein(request.protein());
        if (request.carbs() != null)         menu.setCarbs(request.carbs());
        if (request.fat() != null)           menu.setFat(request.fat());
        if (request.fiber() != null)         menu.setFiber(request.fiber());
        if (request.calories() != null)      menu.setCalories(request.calories());

        saveMenu(menu);
    }

    // Deleta um menu — só ADMIN
    @Transactional
    public void delete(UUID id) {
        Menu menu = findMenuById(id);
        try {
            menuRepository.delete(menu);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao deletar menu com ID " + id + ": " + e.getMessage());
        }
    }

    // --- Métodos privados ---

    private void saveMenu(Menu menu) {
        try {
            menuRepository.save(menu);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao salvar menu: " + e.getMessage());
        }
    }

    private Menu findMenuById(UUID id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Menu com ID " + id + " não encontrado."));
    }

    private Category findCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundExceptions(
                        "Categoria com ID " + categoryId + " não encontrada."));
    }

    private boolean isBlank(String field) {
        return field == null || field.isBlank();
    }
}
