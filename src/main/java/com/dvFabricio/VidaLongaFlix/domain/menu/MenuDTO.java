package com.dvFabricio.VidaLongaFlix.domain.menu;

import com.dvFabricio.VidaLongaFlix.domain.category.CategoryDTO;

import java.util.UUID;

public record MenuDTO(
        UUID id,
        String title,
        String description,
        String cover,
        CategoryDTO category,
        String recipe,
        String nutritionistTips,
        Double protein,
        Double carbs,
        Double fat,
        Double fiber,
        Double calories
) {
    public MenuDTO(Menu menu) {
        this(
                menu.getId(),
                menu.getTitle(),
                menu.getDescription(),
                menu.getCover(),
                menu.getCategory() != null ? new CategoryDTO(menu.getCategory()) : null,
                menu.getRecipe(),
                menu.getNutritionistTips(),
                menu.getProtein(),
                menu.getCarbs(),
                menu.getFat(),
                menu.getFiber(),
                menu.getCalories()
        );
    }
}
