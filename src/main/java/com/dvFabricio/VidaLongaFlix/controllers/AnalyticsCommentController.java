package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/comentarios")
public class AnalyticsCommentController {

    private final CommentService commentService;

    public AnalyticsCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/quantidade/video/{videoId}")
    public ResponseEntity<?> getCommentCountByVideo(@PathVariable UUID videoId) {
        try {
            int count = commentService.getCommentCountByVideo(videoId);
            return ResponseEntity.ok(count);
        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/usuarios/video/{videoId}")
    public ResponseEntity<?> getUserNamesByVideo(@PathVariable UUID videoId) {
        try {
            List<String> userNames = commentService.getUserNamesFromCommentsByVideo(videoId);
            return ResponseEntity.ok(userNames);
        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/total")
    public ResponseEntity<?> getTotalCommentsOnPlatform() {
        try {
            long totalComments = commentService.getTotalCommentsOnPlatform();
            return ResponseEntity.ok(totalComments);
        } catch (DatabaseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/total-por-video")
    public ResponseEntity<?> getTotalCommentsByVideo() {
        try {
            Map<UUID, Long> totalCommentsByVideo = commentService.getTotalCommentsByVideo();
            return ResponseEntity.ok(totalCommentsByVideo);
        } catch (DatabaseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}

