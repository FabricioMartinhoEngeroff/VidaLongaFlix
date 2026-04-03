package com.dvFabricio.VidaLongaFlix.videoTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.AdminVideoController;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ValidationException;
import com.dvFabricio.VidaLongaFlix.services.MediaStorageService;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminVideoMultipartControllerTest {

    @InjectMocks
    private AdminVideoController adminVideoController;

    @Mock
    private VideoService videoService;

    @Mock
    private MediaStorageService mediaStorageService;

    private MockMvc mockMvc;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminVideoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        categoryId = UUID.randomUUID();
    }

    @Test
    void shouldCreateVideoFromMultipartAndPersistGeneratedUrls() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "videoFile", "video.mp4", "video/mp4", "video-content".getBytes());
        MockMultipartFile coverFile = new MockMultipartFile(
                "coverFile", "cover.jpg", "image/jpeg", "cover-content".getBytes());

        when(mediaStorageService.storeVideo(any(), anyString()))
                .thenReturn("https://vidalongaflix.com/api/media/videos/generated-video.mp4");
        when(mediaStorageService.storeCover(any(), anyString()))
                .thenReturn("https://vidalongaflix.com/api/media/covers/generated-cover.jpg");

        mockMvc.perform(multipart("/admin/videos")
                        .file(videoFile)
                        .file(coverFile)
                        .param("title", "Uploaded video")
                        .param("description", "Uploaded description")
                        .param("categoryId", categoryId.toString())
                        .param("recipe", "Recipe")
                        .param("protein", "10.5"))
                .andExpect(status().isCreated());

        ArgumentCaptor<VideoRequestDTO> captor = ArgumentCaptor.forClass(VideoRequestDTO.class);
        verify(videoService).create(captor.capture());

        VideoRequestDTO persistedRequest = captor.getValue();
        assertEquals("https://vidalongaflix.com/api/media/videos/generated-video.mp4", persistedRequest.url());
        assertEquals("https://vidalongaflix.com/api/media/covers/generated-cover.jpg", persistedRequest.cover());
        assertEquals("Uploaded video", persistedRequest.title());
        assertEquals(categoryId, persistedRequest.categoryId());
        assertEquals(10.5, persistedRequest.protein());
    }

    @Test
    void shouldReturn422WhenVideoFileIsInvalid() throws Exception {
        MockMultipartFile invalidVideo = new MockMultipartFile(
                "videoFile", "video.txt", "text/plain", "not-a-video".getBytes());
        MockMultipartFile coverFile = new MockMultipartFile(
                "coverFile", "cover.jpg", "image/jpeg", "cover-content".getBytes());

        when(mediaStorageService.storeVideo(any(), anyString()))
                .thenThrow(new ValidationException(List.of()));

        mockMvc.perform(multipart("/admin/videos")
                        .file(invalidVideo)
                        .file(coverFile)
                        .param("title", "Uploaded video")
                        .param("description", "Uploaded description")
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isUnprocessableEntity());

        verify(videoService, never()).create(any());
    }

    @Test
    void shouldCleanupStoredFilesWhenCreateFailsAfterUpload() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "videoFile", "video.mp4", "video/mp4", "video-content".getBytes());
        MockMultipartFile coverFile = new MockMultipartFile(
                "coverFile", "cover.jpg", "image/jpeg", "cover-content".getBytes());

        String videoUrl = "https://vidalongaflix.com/api/media/videos/generated-video.mp4";
        String coverUrl = "https://vidalongaflix.com/api/media/covers/generated-cover.jpg";

        when(mediaStorageService.storeVideo(any(), anyString())).thenReturn(videoUrl);
        when(mediaStorageService.storeCover(any(), anyString())).thenReturn(coverUrl);
        doThrow(new ValidationException(List.of())).when(videoService).create(any(VideoRequestDTO.class));

        mockMvc.perform(multipart("/admin/videos")
                        .file(videoFile)
                        .file(coverFile)
                        .param("title", "Uploaded video")
                        .param("description", "Uploaded description")
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isUnprocessableEntity());

        verify(mediaStorageService).deleteByPublicUrl(videoUrl);
        verify(mediaStorageService).deleteByPublicUrl(coverUrl);
    }

    @Test
    void shouldReturn400WhenCategoryIdIsMalformedInMultipartRequest() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "videoFile", "video.mp4", "video/mp4", "video-content".getBytes());
        MockMultipartFile coverFile = new MockMultipartFile(
                "coverFile", "cover.jpg", "image/jpeg", "cover-content".getBytes());

        mockMvc.perform(multipart("/admin/videos")
                        .file(videoFile)
                        .file(coverFile)
                        .param("title", "Uploaded video")
                        .param("description", "Uploaded description")
                        .param("categoryId", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid parameter"));
    }
}
