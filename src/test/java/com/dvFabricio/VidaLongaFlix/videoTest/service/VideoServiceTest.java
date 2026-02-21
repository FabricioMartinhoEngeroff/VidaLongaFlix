//package com.dvFabricio.VidaLongaFlix.videoTest.service;
//
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
//import com.dvFabricio.VidaLongaFlix.domain.category.Category;
//import com.dvFabricio.VidaLongaFlix.domain.video.Video;
//import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
//import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
//import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
//import com.dvFabricio.VidaLongaFlix.services.VideoService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//import static org.mockito.Mockito.never;
//
//@ExtendWith(MockitoExtension.class)
//class VideoServiceTest {
//
//    @InjectMocks
//    private VideoService videoService;
//
//    @Mock
//    private VideoRepository videoRepository;
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    private Video video;
//    private VideoDTO videoDTO;
//    private Category category;
//
//    @BeforeEach
//    void setup() {
//        category = new Category();
//        category.setName("Education");
//
//        UUID categoryId = UUID.randomUUID();
//        ReflectionTestUtils.setField(category, "id", categoryId);
//
//        video = Video.builder()
//                .title("Video Title")
//                .description("Video Description")
//                .url("http://example.com/video")
//                .category(category)
//                .views(100)
//                .watchTime(50.5)
//                .build();
//
//        UUID videoId = UUID.randomUUID();
//        ReflectionTestUtils.setField(video, "id", videoId);
//
//        videoDTO = new VideoDTO(
//                videoId, video.getTitle(), video.getDescription(), video.getUrl(),
//                categoryId, List.of(), 0, video.getViews(), video.getWatchTime()
//        );
//    }
//
//    @Test
//    void shouldCreateNewVideo() {
//        UUID categoryId = category.getId();
//        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
//        given(videoRepository.save(any(Video.class))).willReturn(video);
//
//        assertDoesNotThrow(() -> videoService.create(videoDTO));
//
//        then(categoryRepository).should().findById(categoryId);
//        then(videoRepository).should().save(any(Video.class));
//    }
//
//    @Test
//    void shouldThrowExceptionWhenCreatingVideoWithNonExistentCategory() {
//        UUID categoryId = UUID.randomUUID();
//        videoDTO = new VideoDTO(null, "Title", "Description", "http://example.com", categoryId, null, 0, 0, 0.0);
//        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());
//
//        ResourceNotFoundExceptions exception = assertThrows(
//                ResourceNotFoundExceptions.class,
//                () -> videoService.create(videoDTO)
//        );
//
//        assertEquals("Category with ID " + categoryId + " not found.", exception.getMessage());
//        then(categoryRepository).should().findById(categoryId);
//    }
//
//    @Test
//    void shouldUpdateExistingVideo() {
//        UUID videoId = video.getId();
//        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));
//        given(categoryRepository.findById(category.getId())).willReturn(Optional.of(category));
//        given(videoRepository.save(any(Video.class))).willReturn(video);
//
//        VideoDTO updatedDTO = new VideoDTO(
//                videoId, "Updated Title", "Updated Description", "http://example.com/updated",
//                category.getId(), null, 0, 0, 0.0
//        );
//
//        assertDoesNotThrow(() -> videoService.update(videoId, updatedDTO));
//
//        assertEquals("Updated Title", video.getTitle());
//        assertEquals("Updated Description", video.getDescription());
//        assertEquals("http://example.com/updated", video.getUrl());
//        then(videoRepository).should().findById(videoId);
//        then(videoRepository).should().save(any(Video.class));
//    }
//
//    @Test
//    void shouldDeleteExistingVideo() {
//        UUID videoId = video.getId();
//        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));
//
//        assertDoesNotThrow(() -> videoService.delete(videoId));
//
//        then(videoRepository).should().findById(videoId);
//        then(videoRepository).should().delete(video);
//    }
//
//    @Test
//    void shouldNotDeleteVideoWhenNotFound() {
//        UUID videoId = UUID.randomUUID();
//        given(videoRepository.findById(videoId)).willReturn(Optional.empty());
//
//        ResourceNotFoundExceptions exception = assertThrows(
//                ResourceNotFoundExceptions.class,
//                () -> videoService.delete(videoId)
//        );
//
//        assertEquals("Video with ID " + videoId + " not found.", exception.getMessage());
//        then(videoRepository).should().findById(videoId);
//        then(videoRepository).should(never()).delete(any(Video.class));
//    }
//
//    @Test
//    void shouldReturnMostWatchedVideos() {
//        int limit = 10;
//        given(videoRepository.findTopByOrderByViewsDesc(Pageable.ofSize(limit))).willReturn(List.of(video));
//
//        List<VideoDTO> result = videoService.getMostWatchedVideos(limit);
//
//        assertEquals(1, result.size());
//        assertEquals(video.getTitle(), result.get(0).title());
//        then(videoRepository).should().findTopByOrderByViewsDesc(Pageable.ofSize(limit));
//    }
//
//    @Test
//    void shouldReturnLeastWatchedVideos() {
//        int limit = 10;
//        given(videoRepository.findTopByOrderByViewsAsc(Pageable.ofSize(limit))).willReturn(List.of(video));
//
//        List<VideoDTO> result = videoService.getLeastWatchedVideos(limit);
//
//        assertEquals(1, result.size());
//        assertEquals(video.getTitle(), result.get(0).title());
//        then(videoRepository).should().findTopByOrderByViewsAsc(Pageable.ofSize(limit));
//    }
//
//    @Test
//    void shouldReturnTotalViewsByCategory() {
//        given(categoryRepository.findAll()).willReturn(List.of(category));
//        given(videoRepository.countViewsByCategoryId(category.getId())).willReturn(100L);
//
//        Map<String, Long> result = videoService.getTotalViewsByCategory();
//
//        assertEquals(1, result.size());
//        assertEquals(100L, result.get("Education"));
//        then(categoryRepository).should().findAll();
//        then(videoRepository).should().countViewsByCategoryId(category.getId());
//    }
//
//    @Test
//    void shouldReturnAverageWatchTimeForVideo() {
//        UUID videoId = video.getId();
//        given(videoRepository.findAverageWatchTimeByVideoId(videoId)).willReturn(Optional.of(30.5));
//
//        double result = videoService.getAverageWatchTime(videoId);
//
//        assertEquals(30.5, result);
//        then(videoRepository).should().findAverageWatchTimeByVideoId(videoId);
//    }
//
//    @Test
//    void shouldReturnMostCommentedVideos() {
//        int limit = 5;
//        given(videoRepository.findTopByOrderByCommentsCountDesc(Pageable.ofSize(limit))).willReturn(List.of(video));
//
//        List<VideoDTO> result = videoService.getVideosWithMostComments(limit);
//
//        assertEquals(1, result.size());
//        assertEquals(video.getTitle(), result.get(0).title());
//        then(videoRepository).should().findTopByOrderByCommentsCountDesc(Pageable.ofSize(limit));
//    }
//
//    @Test
//    void shouldThrowExceptionWhenFindingNonExistentVideoById() {
//        UUID videoId = UUID.randomUUID();
//        given(videoRepository.findById(videoId)).willReturn(Optional.empty());
//
//        ResourceNotFoundExceptions exception = assertThrows(
//                ResourceNotFoundExceptions.class,
//                () -> videoService.findById(videoId)
//        );
//
//        assertEquals("Video with ID " + videoId + " not found.", exception.getMessage());
//        then(videoRepository).should().findById(videoId);
//    }
//}
