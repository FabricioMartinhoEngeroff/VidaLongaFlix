package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/comentarios")
@RequiredArgsConstructor
public class AnalyticsCommentController {

    private final CommentService commentService;

    @GetMapping("/quantidade/video/{videoId}")
    public ResponseEntity<Integer> getCommentCountByVideo(@PathVariable UUID videoId) {
        int count = commentService.getCommentCountByVideo(videoId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/usuarios/video/{videoId}")
    public ResponseEntity<List<String>> getUserNamesByVideo(@PathVariable UUID videoId) {
        return ResponseEntity.ok(commentService.getUserNamesFromCommentsByVideo(videoId));
    }

    @GetMapping("/total")
    public ResponseEntity<Long> getTotalCommentsOnPlatform() {
        return ResponseEntity.ok(commentService.getTotalCommentsOnPlatform());
    }

    @GetMapping("/total-por-video")
    public ResponseEntity<Map<UUID, Long>> getTotalCommentsByVideo() {
        return ResponseEntity.ok(commentService.getTotalCommentsByVideo());
    }
}