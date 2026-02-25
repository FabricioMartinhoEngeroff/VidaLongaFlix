package com.dvFabricio.VidaLongaFlix.domain.menu;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;

import java.util.UUID;

public record MenuDTO(
        UUID id,
        String title,
        String description,
        String cover,
        Category category,
        String recipe,
        String nutritionistTips,
        Double protein,
        Double carbs,
        Double fat,
        Double fiber,
        Double calories
) {
    // Construtor que converte entidade Menu em MenuDTO
    public MenuDTO(Menu menu) {
        this(
                menu.getId(),
                menu.getTitle(),
                menu.getDescription(),
                menu.getCover(),
                menu.getCategory(),
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
