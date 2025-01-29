package com.dvFabricio.VidaLongaFlix.videoTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.video.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class VideoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;
    private Video video1;
    private Video video2;

    @BeforeEach
    void setup() {

        videoRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("Education");
        categoryRepository.saveAndFlush(category);

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

        videoRepository.saveAllAndFlush(List.of(video1, video2));
    }

    @Test
    void findTopByOrderByViewsDesc_ShouldReturnMostViewedVideos() {
        Pageable limit = PageRequest.of(0, 1);
        List<Video> result = videoRepository.findTopByOrderByViewsDesc(limit);

        assertAll(
                () -> assertEquals(1, result.size(), "Should return only 1 video"),
                () -> assertEquals(video1.getTitle(), result.get(0).getTitle(), "The most viewed video should be 'Video 1'")
        );
    }

    @Test
    void findTopByOrderByViewsAsc_ShouldReturnLeastViewedVideos() {
        Pageable limit = PageRequest.of(0, 1);
        List<Video> result = videoRepository.findTopByOrderByViewsAsc(limit);

        assertAll(
                () -> assertEquals(1, result.size(), "Should return only 1 video"),
                () -> assertEquals(video2.getTitle(), result.get(0).getTitle(), "The least viewed video should be 'Video 2'")
        );
    }

    @Test
    void countViewsByCategoryId_ShouldReturnTotalViewsForCategory() {
        Long totalViews = videoRepository.countViewsByCategoryId(category.getId());

        assertEquals(300L, totalViews, "The total views should be 300");
    }

    @Test
    void findAverageWatchTimeByVideoId_ShouldReturnCorrectAverage() {
        Optional<Double> averageWatchTime = videoRepository.findAverageWatchTimeByVideoId(video1.getId());

        assertAll(
                () -> assertTrue(averageWatchTime.isPresent(), "The average watch time should be present"),
                () -> assertEquals(120.0, averageWatchTime.get(), "The average watch time should be 120.0")
        );
    }

    @Test
    void findTopByOrderByCommentsCountDesc_ShouldReturnVideosWithMostComments() {
        Pageable limit = PageRequest.of(0, 1);
        List<Video> result = videoRepository.findTopByOrderByCommentsCountDesc(limit);

        assertEquals(1, result.size(), "Should return only 1 video");
        assertTrue(result.contains(video1), "The video should be 'Video 1'");
    }

    @Test
    void shouldReturnEmptyResultsWhenNoVideosExist() {
        videoRepository.deleteAll();

        List<Video> topVideos = videoRepository.findTopByOrderByViewsDesc(Pageable.ofSize(1));
        Optional<Double> avgWatchTime = videoRepository.findAverageWatchTimeByVideoId(UUID.randomUUID());

        assertAll(
                () -> assertTrue(topVideos.isEmpty(), "No video should be returned"),
                () -> assertFalse(avgWatchTime.isPresent(), "No average watch time should be returned")
        );
    }
}