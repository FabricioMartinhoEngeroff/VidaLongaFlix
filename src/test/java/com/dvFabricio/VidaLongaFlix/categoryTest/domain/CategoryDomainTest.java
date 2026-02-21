package com.dvFabricio.VidaLongaFlix.categoryTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryDomainTest {

    private final Validator validator;

    public CategoryDomainTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void shouldCreateCategoryWithValidData() {
        Category category = new Category("Health", CategoryType.VIDEO);

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertTrue(violations.isEmpty());
        assertEquals("Health", category.getName());
        assertEquals(CategoryType.VIDEO, category.getType());
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        Category category = new Category("", CategoryType.VIDEO);

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenNameExceedsMaxLength() {
        Category category = new Category("a".repeat(101), CategoryType.VIDEO);

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertFalse(violations.isEmpty());
    }
}