//package com.dvFabricio.VidaLongaFlix.videoTest.controller;
//
//import com.dvFabricio.VidaLongaFlix.controllers.VideoController;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
//import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
//import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
//import com.dvFabricio.VidaLongaFlix.services.VideoService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//public class VideoControllerTest {
//
//    private MockMvc mockMvc;
//
//    @InjectMocks
//    private VideoController videoController;
//
//    @Mock
//    private VideoService videoService;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(videoController).build();
//    }
//
//    @Test
//    void shouldReturnAllVideos() throws Exception {
//        List<VideoDTO> videos = List.of(
//                new VideoDTO(UUID.randomUUID(), "Video 1", "Description 1", "http://example.com/1", UUID.randomUUID(), List.of(), 0, 100, 10.5),
//                new VideoDTO(UUID.randomUUID(), "Video 2", "Description 2", "http://example.com/2", UUID.randomUUID(), List.of(), 0, 200, 20.5)
//        );
//
//        when(videoService.findAll()).thenReturn(videos);
//
//        mockMvc.perform(get("/videos"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(2))
//                .andExpect(jsonPath("$[0].title").value("Video 1"))
//                .andExpect(jsonPath("$[1].title").value("Video 2"));
//
//        verify(videoService).findAll();
//    }
//
//    @Test
//    void shouldReturnEmptyListWhenNoVideosExist() throws Exception {
//        when(videoService.findAll()).thenReturn(List.of());
//
//        mockMvc.perform(get("/videos"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(0));
//
//        verify(videoService).findAll();
//    }
//
//    @Test
//    void shouldReturnVideoById() throws Exception {
//        UUID videoId = UUID.randomUUID();
//        VideoDTO videoDTO = new VideoDTO(videoId, "Video 1", "Description 1", "http://example.com/1", UUID.randomUUID(), List.of(), 0, 100, 10.5);
//
//        when(videoService.findById(videoId)).thenReturn(videoDTO);
//
//        mockMvc.perform(get("/videos/{id}", videoId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.title").value("Video 1"));
//
//        verify(videoService).findById(videoId);
//    }
//
//    @Test
//    void shouldReturnNotFoundWhenVideoDoesNotExist() throws Exception {
//        UUID videoId = UUID.randomUUID();
//
//        when(videoService.findById(videoId)).thenThrow(new ResourceNotFoundExceptions("Video with ID " + videoId + " not found."));
//
//        mockMvc.perform(get("/videos/{id}", videoId))
//                .andExpect(status().isNotFound())
//                .andExpect(content().string("Video with ID " + videoId + " not found."));
//
//        verify(videoService).findById(videoId);
//    }
//
//    @Test
//    void shouldCreateVideo() throws Exception {
//        UUID categoryId = UUID.randomUUID();
//        VideoDTO videoDTO = new VideoDTO(null, "Video 1", "Description 1", "http://example.com/1", categoryId, List.of(), 0, 100, 10.5);
//
//        doNothing().when(videoService).create(any(VideoDTO.class));
//
//        mockMvc.perform(post("/videos")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{" +
//                                "\"title\":\"Video 1\"," +
//                                "\"description\":\"Description 1\"," +
//                                "\"url\":\"http://example.com/1\"," +
//                                "\"categoryId\":\"" + categoryId + "\"}"))
//                .andExpect(status().isCreated());
//
//        verify(videoService).create(any(VideoDTO.class));
//    }
//
//    @Test
//    void shouldReturnBadRequestWhenCreatingVideoWithNonExistentCategory() throws Exception {
//        UUID categoryId = UUID.randomUUID();
//
//        doThrow(new ResourceNotFoundExceptions("Category with ID " + categoryId + " not found."))
//                .when(videoService).create(any(VideoDTO.class));
//
//        mockMvc.perform(post("/videos")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{" +
//                                "\"title\":\"Video 1\"," +
//                                "\"description\":\"Description 1\"," +
//                                "\"url\":\"http://example.com/1\"," +
//                                "\"categoryId\":\"" + categoryId + "\"}"))
//                .andExpect(status().isNotFound())
//                .andExpect(content().string("Category with ID " + categoryId + " not found."));
//
//        verify(videoService).create(any(VideoDTO.class));
//    }
//
//    @Test
//    void shouldUpdateVideo() throws Exception {
//        UUID videoId = UUID.randomUUID();
//        UUID categoryId = UUID.randomUUID();
//
//        mockMvc.perform(put("/videos/{id}", videoId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{" +
//                                "\"title\":\"Updated Video\"," +
//                                "\"description\":\"Updated Description\"," +
//                                "\"url\":\"http://example.com/updated\"," +
//                                "\"categoryId\":\"" + categoryId + "\"}"))
//                .andExpect(status().isOk());
//
//        verify(videoService).update(eq(videoId), any(VideoDTO.class));
//    }
//
//    @Test
//    void shouldReturnNotFoundWhenUpdatingNonExistentVideo() throws Exception {
//        UUID videoId = UUID.randomUUID();
//        doThrow(new ResourceNotFoundExceptions("Video not found")).when(videoService).update(eq(videoId), any(VideoDTO.class));
//
//        mockMvc.perform(put("/videos/{id}", videoId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{" +
//                                "\"title\":\"Updated Video\"," +
//                                "\"description\":\"Updated Description\"," +
//                                "\"url\":\"http://example.com/updated\"}"))
//                .andExpect(status().isNotFound())
//                .andExpect(content().string("Video not found"));
//
//        verify(videoService).update(eq(videoId), any(VideoDTO.class));
//    }
//
//    @Test
//    void shouldDeleteVideo() throws Exception {
//        UUID videoId = UUID.randomUUID();
//
//        doNothing().when(videoService).delete(videoId);
//
//        mockMvc.perform(delete("/videos/{id}", videoId))
//                .andExpect(status().isNoContent());
//
//        verify(videoService).delete(videoId);
//    }
//
//    @Test
//    void shouldReturnNotFoundWhenDeletingNonExistentVideo() throws Exception {
//        UUID videoId = UUID.randomUUID();
//
//        doThrow(new ResourceNotFoundExceptions("Video with ID " + videoId + " not found."))
//                .when(videoService).delete(videoId);
//
//        mockMvc.perform(delete("/videos/{id}", videoId))
//                .andExpect(status().isNotFound())
//                .andExpect(content().string("Video with ID " + videoId + " not found."));
//
//        verify(videoService).delete(videoId);
//    }
//
//    @Test
//    void shouldReturnInternalServerErrorWhenDatabaseErrorOccurs() throws Exception {
//        UUID videoId = UUID.randomUUID();
//        doThrow(new DatabaseException("Database error")).when(videoService).delete(videoId);
//
//        mockMvc.perform(delete("/videos/{id}", videoId))
//                .andExpect(status().isInternalServerError())
//                .andExpect(content().string("Database error"));
//
//        verify(videoService).delete(videoId);
//    }
//}
