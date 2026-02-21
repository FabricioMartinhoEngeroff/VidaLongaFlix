package com.dvFabricio.VidaLongaFlix.controllers;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CommentResponseDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.CreateCommentDTO;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.services.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Void> createComment(
            @RequestBody @Valid CreateCommentDTO dto,
            @AuthenticationPrincipal User user) {
        commentService.create(dto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByVideo(@PathVariable UUID videoId) {
        return ResponseEntity.ok(commentService.getCommentsByVideo(videoId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(commentService.getCommentsByUser(userId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
