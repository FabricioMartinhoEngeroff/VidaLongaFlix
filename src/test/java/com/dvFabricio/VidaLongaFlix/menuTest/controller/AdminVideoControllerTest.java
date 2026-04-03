package com.dvFabricio.VidaLongaFlix.menuTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.AdminVideoController;
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
class AdminVideoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AdminVideoController adminVideoController;
    @Mock private VideoService videoService;

    private UUID videoId;
    private UUID categoryId;
    private VideoDTO videoDTO;
    private VideoRequestDTO videoRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminVideoController)
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

        videoRequest = new VideoRequestDTO(
                "Video 1", "Desc 1", "http://example.com",
                "http://cover.com", categoryId,
                null, null, null, null, null, null);
    }

    @Test
    void shouldCreateVideo() throws Exception {
        doNothing().when(videoService).create(any(VideoRequestDTO.class));

        mockMvc.perform(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(videoRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBadRequestWhenCreatingWithMissingFields() throws Exception {
        // Request com título vazio — vai falhar no @Valid
        VideoRequestDTO invalidRequest = new VideoRequestDTO(
                "", "Desc", "http://url.com",
                null, categoryId,
                null, null, null, null, null, null);

        mockMvc.perform(post("/admin/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateVideo() throws Exception {
        doNothing().when(videoService).update(eq(videoId), any(VideoRequestDTO.class));
        when(videoService.findById(videoId)).thenReturn(videoDTO);

        mockMvc.perform(put("/admin/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(videoRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Video 1"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentVideo() throws Exception {
        doThrow(new ResourceNotFoundExceptions(
                "Video with ID " + videoId + " not found."))
                .when(videoService).update(eq(videoId), any(VideoRequestDTO.class));

        mockMvc.perform(put("/admin/videos/{id}", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(videoRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteVideo() throws Exception {
        doNothing().when(videoService).delete(videoId);

        mockMvc.perform(delete("/admin/videos/{id}", videoId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentVideo() throws Exception {
        doThrow(new ResourceNotFoundExceptions(
                "Video with ID " + videoId + " not found."))
                .when(videoService).delete(videoId);

        mockMvc.perform(delete("/admin/videos/{id}", videoId))
                .andExpect(status().isNotFound());
    }
}