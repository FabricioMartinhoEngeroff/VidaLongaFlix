package com.dvFabricio.VidaLongaFlix.categoryTest.domain;


import com.dvFabricio.VidaLongaFlix.domain.video.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class CategoryTest {

    private final Validator validator;

    public CategoryTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void shouldCreateCategoryWithValidName() {
        Category category = new Category("Health");

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertTrue(violations.isEmpty(), "No validation errors should occur for a valid category.");
        assertEquals("Health", category.getName(), "The category name should be set correctly.");
    }

    @Test
    void shouldNotCreateCategoryWithBlankName() {
        Category category = new Category("");

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertFalse(violations.isEmpty(), "Validation errors should occur for a blank name.");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("The category name cannot be empty.")),
                "The error message for blank name should match.");
    }

    @Test
    void shouldNotCreateCategoryWithNameExceedingMaxLength() {
        String longName = "a".repeat(101);
        Category category = new Category(longName);

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertFalse(violations.isEmpty(), "Validation errors should occur for a name exceeding max length.");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("The category name cannot exceed 100 characters.")),
                "The error message for exceeding max length should match.");
    }

    @Test
    void shouldInitializeWithEmptyVideosSet() {
        Category category = new Category("Health");

        assertNotNull(category.getVideos(), "The videos set should be initialized.");
        assertTrue(category.getVideos().isEmpty(), "The videos set should initially be empty.");
    }

    @Test
    void shouldAddVideoToCategory() {
        Category category = new Category("Health");
        Video video = new Video();

        category.getVideos().add(video);

        assertTrue(category.getVideos().contains(video), "The video should be added to the category.");
        assertEquals(1, category.getVideos().size(), "The category should contain one video.");
    }

    @Test
    void shouldRemoveVideoFromCategory() {
        Category category = new Category("Health");
        Video video = new Video();

        category.getVideos().add(video);
        category.getVideos().remove(video);

        assertFalse(category.getVideos().contains(video), "The video should be removed from the category.");
        assertTrue(category.getVideos().isEmpty(), "The category should have no videos after removal.");
    }

}
