//package com.dvFabricio.VidaLongaFlix.commentTest.controller;
//
//
//import com.dvFabricio.VidaLongaFlix.controllers.CommentController;
//import com.dvFabricio.VidaLongaFlix.domain.DTOs.CreateCommentDTO;
//import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
//import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
//import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
//import com.dvFabricio.VidaLongaFlix.services.CommentService;
//import com.fasterxml.jackson.databind.ObjectMapper;
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
//import java.util.UUID;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@ExtendWith(MockitoExtension.class)
//public class CommentControllerTest {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private MockMvc mockMvc;
//
//    @InjectMocks
//    private CommentController commentController;
//
//    @Mock
//    private CommentService commentService;
//
//    private UUID commentId;
//    private UUID videoId;
//    private UUID userId;
//    private CreateCommentDTO createCommentDTO;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
//
//        commentId = UUID.randomUUID();
//        videoId = UUID.randomUUID();
//        userId = UUID.randomUUID();
//
//        createCommentDTO = new CreateCommentDTO(commentId, "Great video!", null, userId, videoId);
//    }
//
//    @Test
//    void shouldCreateComment() throws Exception {
//        doNothing().when(commentService).create(any(CreateCommentDTO.class));
//
//        mockMvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new CreateCommentDTO(null, "Great video!", null, userId, videoId)))).andExpect(status().isCreated());
//
//        verify(commentService).create(any(CreateCommentDTO.class));
//    }
//
//    @Test
//    void shouldReturnConflictWhenCommentExists() throws Exception {
//        doThrow(new DuplicateResourceException("Duplicate comment", "Duplicate comment")).when(commentService).create(any(CreateCommentDTO.class));
//
//        mockMvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new CreateCommentDTO(null, "Great video!", null, userId, videoId)))).andExpect(status().isConflict()).andExpect(content().string("Comment: Duplicate comment"));
//
//        verify(commentService).create(any(CreateCommentDTO.class));
//    }
//
//
//    @Test
//    void shouldReturnNotFoundWhenDeletingNonExistentComment() throws Exception {
//        doThrow(new ResourceNotFoundExceptions("Comment not found")).when(commentService).delete(commentId);
//
//        mockMvc.perform(delete("/comments/{commentId}", commentId)).andExpect(status().isNotFound()).andExpect(content().string("Comment not found")); // ✅ Fix: Ensure expected response
//
//        verify(commentService).delete(commentId);
//    }
//
//    @Test
//    void shouldReturnInternalServerError() throws Exception {
//        doThrow(new DatabaseException("Database error")).when(commentService).delete(commentId);
//
//        mockMvc.perform(delete("/comments/{commentId}", commentId)).andExpect(status().isInternalServerError()).andExpect(content().string("Database error")); // ✅ Fix: Ensure correct error response
//
//        verify(commentService).delete(commentId);
//    }
//
//    @Test
//    void shouldReturnBadRequestForInvalidComment() throws Exception {
//        doThrow(new IllegalArgumentException("Invalid data"))
//                .when(commentService).create(any(CreateCommentDTO.class));
//
//        mockMvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new CreateCommentDTO(null, "", null, userId, videoId)))).andExpect(status().isBadRequest()) // ✅ Ensure it correctly returns 400
//                .andExpect(content().string("Invalid data"));
//
//        verify(commentService).create(any(CreateCommentDTO.class));
//    }
//
//
//    @Test
//    void shouldReturnNotFoundWhenVideoDoesNotExist() throws Exception {
//        when(commentService.getCommentsByVideo(videoId)).thenThrow(new ResourceNotFoundExceptions("Video not found"));
//
//        mockMvc.perform(get("/comments/video/{videoId}", videoId)).andExpect(status().isNotFound()).andExpect(content().string("Video not found")); // ✅ Fix: Ensure plain message, not JSON list
//
//        verify(commentService).getCommentsByVideo(videoId);
//    }
//}