package com.dvFabricio.VidaLongaFlix.commentTest.controller;

import com.dvFabricio.VidaLongaFlix.controllers.CommentController;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CreateCommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.GlobalExceptionHandler;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private CommentController commentController;
    @Mock private CommentService commentService;

    private User user;
    private UUID userId;
    private UUID videoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        user = new User("user1", "user1@example.com", "password", "11999999999");
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        videoId = UUID.randomUUID();

    }

    @Test
    void shouldCreateComment() throws Exception {
        doNothing().when(commentService)
                .create(any(CreateCommentDTO.class), any(UUID.class)); // ← any() nos dois

        mockMvc.perform(post("/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCommentDTO("Ótimo vídeo!", videoId))))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnNotFoundWhenVideoDoesNotExist() throws Exception {
        when(commentService.getCommentsByVideo(videoId))
                .thenThrow(new ResourceNotFoundExceptions("Video not found"));

        mockMvc.perform(get("/comments/video/{videoId}", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        doNothing().when(commentService).delete(commentId);

        mockMvc.perform(delete("/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());
    }
}