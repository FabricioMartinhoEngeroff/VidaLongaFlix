package com.dvFabricio.VidaLongaFlix.videoTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.VideoController;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryDTO;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

    private MockMvc mockMvc;

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
                .thenThrow(new ResourceNotFoundExceptions(
                        "Video with ID " + videoId + " not found."));

        mockMvc.perform(get("/videos/{id}", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRegisterView() throws Exception {
        doNothing().when(videoService).registerView(videoId);

        mockMvc.perform(patch("/videos/{id}/view", videoId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenRegisteringViewForNonExistentVideo() throws Exception {
        doThrow(new ResourceNotFoundExceptions(
                "Video with ID " + videoId + " not found."))
                .when(videoService).registerView(videoId);

        mockMvc.perform(patch("/videos/{id}/view", videoId))
                .andExpect(status().isNotFound());
    }
}