package com.dvFabricio.VidaLongaFlix.videoTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoTest {

    @Test
    void shouldCreateVideoWithBuilder() {
        Category category = new Category("Education", CategoryType.VIDEO);

        Video video = Video.builder()
                .title("Java Basics")
                .description("Intro to Java")
                .url("http://example.com/video")
                .category(category)
                .views(100)
                .watchTime(50.0)
                .build();

        assertEquals("Java Basics", video.getTitle());
        assertEquals("Intro to Java", video.getDescription());
        assertEquals(category, video.getCategory());
        assertEquals(100, video.getViews());
        assertEquals(50.0, video.getWatchTime());
    }

    @Test
    void shouldHaveDefaultValues() {
        Category category = new Category("Education", CategoryType.VIDEO);

        Video video = Video.builder()
                .title("Title").description("Desc")
                .url("http://example.com")
                .category(category)
                .views(0).watchTime(0).build();

        assertEquals(0, video.getViews());
        assertEquals(0.0, video.getWatchTime());
        assertEquals(0, video.getLikesCount());
        assertFalse(video.isFavorited());
        assertTrue(video.getComments().isEmpty());
    }

    @Test
    void shouldUpdateProperties() {
        Category category = new Category("Education", CategoryType.VIDEO);

        Video video = Video.builder()
                .title("Old Title").description("Old Desc")
                .url("http://old.com").category(category)
                .views(0).watchTime(0).build();

        video.setTitle("New Title");
        video.setViews(200);

        assertEquals("New Title", video.getTitle());
        assertEquals(200, video.getViews());
    }
}