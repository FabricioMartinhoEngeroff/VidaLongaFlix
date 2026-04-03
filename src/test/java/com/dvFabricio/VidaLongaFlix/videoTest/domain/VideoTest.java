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
                .cover("http://example.com/cover.jpg")
                .category(category)
                .views(100)
                .watchTime(50.0)
                .build();

        assertEquals("Java Basics", video.getTitle());
        assertEquals("Intro to Java", video.getDescription());
        assertEquals("http://example.com/video", video.getUrl());
        assertEquals("http://example.com/cover.jpg", video.getCover());
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
                .cover("http://example.com/cover.jpg")
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
                .url("http://old.com")
                .cover("http://old.com/cover.jpg")
                .category(category)
                .views(0).watchTime(0).build();

        video.setTitle("New Title");
        video.setUrl("http://new.com/video.mp4");
        video.setCover("http://new.com/cover.jpg");
        video.setViews(200);

        assertEquals("New Title", video.getTitle());
        assertEquals("http://new.com/video.mp4", video.getUrl());
        assertEquals("http://new.com/cover.jpg", video.getCover());
        assertEquals(200, video.getViews());
    }

    @Test
    void shouldAllowPublicMediaUrlsGeneratedByUploadFlow() {
        Category category = new Category("Education", CategoryType.VIDEO);

        Video video = Video.builder()
                .title("Uploaded video")
                .description("Video stored by backend")
                .url("https://vidalongaflix.com/api/media/videos/generated-video.mp4")
                .cover("https://vidalongaflix.com/api/media/covers/generated-cover.jpg")
                .category(category)
                .build();

        assertAll(
                () -> assertTrue(video.getUrl().contains("/api/media/videos/")),
                () -> assertTrue(video.getCover().contains("/api/media/covers/"))
        );
    }
}
