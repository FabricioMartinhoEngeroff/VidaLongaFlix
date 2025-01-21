package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentDTO;
import com.dvFabricio.VidaLongaFlix.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody @Valid CommentDTO commentDTO) {
        try {
            CommentDTO createdComment = commentService.createComment(commentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/video/{videoUuid}")
    public ResponseEntity<?> getCommentsByVideo(@PathVariable UUID videoUuid) {
        try {
            List<CommentDTO> comments = commentService.getCommentsByVideo(videoUuid);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userUuid}")
    public ResponseEntity<?> getCommentsByUser(@PathVariable UUID userUuid) {
        try {
            List<CommentDTO> comments = commentService.getCommentsByUser(userUuid);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{commentUuid}")
    public ResponseEntity<?> deleteComment(@PathVariable UUID commentUuid) {
        try {
            commentService.deleteComment(commentUuid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
