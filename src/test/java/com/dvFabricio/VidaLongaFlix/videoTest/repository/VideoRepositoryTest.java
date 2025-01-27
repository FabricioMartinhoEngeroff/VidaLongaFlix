package com.dvFabricio.VidaLongaFlix.videoTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VideoRepositoryTest {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Video video1;
    private Video video2;
    private Category category;

    @BeforeEach
    void setup() {
        category = new Category();
        category.setName("Education");
        categoryRepository.save(category);

        video1 = Video.builder()
                .title("Video 1")
                .description("Description 1")
                .url("http://example.com/video1")
                .category(category)
                .views(200)
                .watchTime(120.0)
                .build();

        video2 = Video.builder()
                .title("Video 2")
                .description("Description 2")
                .url("http://example.com/video2")
                .category(category)
                .views(100)
                .watchTime(80.0)
                .build();

        videoRepository.saveAll(List.of(video1, video2));
    }

    @Test
    void shouldFindTopVideosByViewsDesc() {
        Pageable limit = PageRequest.of(0, 1);
        List<Video> result = videoRepository.findTopByOrderByViewsDesc(limit);

        assertEquals(1, result.size());
        assertEquals(video1.getTitle(), result.get(0).getTitle());
    }

    @Test
    void shouldFindTopVideosByViewsAsc() {
        Pageable limit = PageRequest.of(0, 1);
        List<Video> result = videoRepository.findTopByOrderByViewsAsc(limit);

        assertEquals(1, result.size());
        assertEquals(video2.getTitle(), result.get(0).getTitle());
    }

    @Test
    void shouldCountViewsByCategoryId() {
        Long totalViews = videoRepository.countViewsByCategoryId(category.getId());

        assertEquals(300L, totalViews);
    }

    @Test
    void shouldFindAverageWatchTimeByVideoId() {
        Optional<Double> averageWatchTime = videoRepository.findAverageWatchTimeByVideoId(video1.getId());

        assertTrue(averageWatchTime.isPresent());
        assertEquals(120.0, averageWatchTime.get());
    }

    @Test
    void shouldFindTopVideosByCommentsCountDesc() {
        Pageable limit = PageRequest.of(0, 1);
        List<Video> result = videoRepository.findTopByOrderByCommentsCountDesc(limit);

        assertEquals(1, result.size());
        assertTrue(result.contains(video1));
    }

    @Test
    void shouldReturnEmptyWhenNoVideos() {
        videoRepository.deleteAll();

        List<Video> topVideos = videoRepository.findTopByOrderByViewsDesc(Pageable.ofSize(1));
        assertTrue(topVideos.isEmpty());

        Optional<Double> avgWatchTime = videoRepository.findAverageWatchTimeByVideoId(UUID.randomUUID());
        assertFalse(avgWatchTime.isPresent());
    }
}
