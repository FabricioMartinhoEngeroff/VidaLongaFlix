package com.dvFabricio.VidaLongaFlix.videoTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.VideoController;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private VideoController videoController;
    @Mock private VideoService videoService;

    private UUID videoId;
    private UUID categoryId;
    private VideoDTO videoDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(videoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        videoId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        videoDTO = new VideoDTO(
                videoId, "Video 1", "Desc 1",
                "http://example.com", "http://cover.com",
                new CategoryDTO(categoryId, "Education", CategoryType.VIDEO),
                List.of(), 0, 100, 50.0,
                null, null, null, null, null, null,
                0, false
        );
    }

    @Test
    void shouldReturnAllVideos() throws Exception {
        when(videoService.findAll()).thenReturn(List.of(videoDTO));

        mockMvc.perform(get("/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].title").value("Video 1"));
    }

    @Test
    void shouldReturnVideoById() throws Exception {
        when(videoService.findById(videoId)).thenReturn(videoDTO);

        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Video 1"));
    }

    @Test
    void shouldReturnNotFoundWhenVideoDoesNotExist() throws Exception {
        when(videoService.findById(videoId))
                .thenThrow(new ResourceNotFoundExceptions("Video with ID " + videoId + " not found."));

        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateVideo() throws Exception {
        VideoRequestDTO request = new VideoRequestDTO(
                "Video 1", "Desc 1", "http://example.com",
                "http://cover.com", categoryId,
                null, null, null, null, null, null);

        doNothing().when(videoService).create(any(VideoRequestDTO.class));

        mockMvc.perform(post("/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDeleteVideo() throws Exception {
        doNothing().when(videoService).delete(videoId);

        mockMvc.perform(delete("/videos/{id}", videoId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistent() throws Exception {
        doThrow(new ResourceNotFoundExceptions("Video with ID " + videoId + " not found."))
                .when(videoService).delete(videoId);

        mockMvc.perform(delete("/videos/{id}", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRegisterView() throws Exception {
        doNothing().when(videoService).registerView(videoId);

        mockMvc.perform(patch("/videos/{id}/view", videoId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateVideo() throws Exception {
        VideoRequestDTO request = new VideoRequestDTO(
                "Updated", "Updated Desc", "http://new.com",
                "http://cover.com", categoryId,
                null, null, null, null, null, null);

        doNothing().when(videoService).update(eq(videoId), any(VideoRequestDTO.class));

        mockMvc.perform(put("/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}