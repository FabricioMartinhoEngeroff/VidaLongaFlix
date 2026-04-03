package com.dvFabricio.VidaLongaFlix.videoTest.service;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ValidationException;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import com.dvFabricio.VidaLongaFlix.services.NotificationService;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @InjectMocks private VideoService videoService;
    @Mock private VideoRepository videoRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private NotificationService notificationService;

    private Video video;
    private Category category;
    private UUID videoId;
    private UUID categoryId;

    @BeforeEach
    void setup() {
        categoryId = UUID.randomUUID();
        category = new Category("Education", CategoryType.VIDEO);
        ReflectionTestUtils.setField(category, "id", categoryId);

        video = Video.builder()
                .title("Video Title")
                .description("Video Description")
                .url("http://example.com/video")
                .category(category)
                .views(100)
                .watchTime(50.5)
                .build();

        videoId = UUID.randomUUID();
        ReflectionTestUtils.setField(video, "id", videoId);
    }

    @Test
    void shouldFindAll() {
        given(videoRepository.findAll()).willReturn(List.of(video));

        List<VideoDTO> result = videoService.findAll();

        assertEquals(1, result.size());
        assertEquals("Video Title", result.get(0).title());
        then(videoRepository).should().findAll();
    }

    @Test
    void shouldFindById() {
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        VideoDTO result = videoService.findById(videoId);

        assertEquals("Video Title", result.title());
    }

    @Test
    void shouldThrowWhenVideoNotFound() {
        given(videoRepository.findById(videoId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> videoService.findById(videoId));
    }

    @Test
    void shouldCreateVideo() {
        VideoRequestDTO request = new VideoRequestDTO(
                "Video Title", "Video Description",
                "http://example.com", "http://cover.com",
                categoryId, null, null, null, null, null, null);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        assertDoesNotThrow(() -> videoService.create(request));
        then(videoRepository).should().save(any(Video.class));
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnCreate() {
        VideoRequestDTO request = new VideoRequestDTO(
                "Title", "Desc", "http://url.com", "http://cover.com",
                categoryId, null, null, null, null, null, null);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> videoService.create(request));
        then(videoRepository).should(never()).save(any());
    }

    @Test
    void shouldRejectBlobVideoUrlOnCreate() {
        VideoRequestDTO request = new VideoRequestDTO(
                "Title", "Desc", "blob:https://vidalongaflix.com/123", "https://cover.com/image.jpg",
                categoryId, null, null, null, null, null, null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> videoService.create(request));

        assertEquals("url", exception.getFieldMessages().get(0).fieldName());
        then(videoRepository).should(never()).save(any());
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectDataUrlCoverOnCreate() {
        VideoRequestDTO request = new VideoRequestDTO(
                "Title", "Desc", "https://cdn.example.com/video.mp4", "data:image/png;base64,abc123",
                categoryId, null, null, null, null, null, null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> videoService.create(request));

        assertEquals("cover", exception.getFieldMessages().get(0).fieldName());
        then(videoRepository).should(never()).save(any());
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldUpdateVideo() {
        VideoRequestDTO request = new VideoRequestDTO(
                "Updated Title", "Updated Desc",
                "http://new.com", "http://cover.com",
                null, null, null, null, null, null, null);

        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        assertDoesNotThrow(() -> videoService.update(videoId, request));
        assertEquals("Updated Title", video.getTitle());
        then(videoRepository).should().save(video);
    }

    @Test
    void shouldRejectLocalCoverPathOnUpdate() {
        VideoRequestDTO request = new VideoRequestDTO(
                "Updated Title", "Updated Desc",
                "https://cdn.example.com/video.mp4", "C:/Users/Fabricio/Downloads/capa.jpg",
                null, null, null, null, null, null, null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> videoService.update(videoId, request));

        assertEquals("cover", exception.getFieldMessages().get(0).fieldName());
        then(videoRepository).should(never()).save(any());
    }

    @Test
    void shouldDeleteVideo() {
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        assertDoesNotThrow(() -> videoService.delete(videoId));
        then(videoRepository).should().delete(video);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentVideo() {
        given(videoRepository.findById(videoId)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundExceptions.class,
                () -> videoService.delete(videoId));
    }

    @Test
    void shouldRegisterView() {
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        videoService.registerView(videoId);

        assertEquals(101, video.getViews());
        then(videoRepository).should().save(video);
    }
}
