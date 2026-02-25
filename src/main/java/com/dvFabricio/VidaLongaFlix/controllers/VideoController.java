package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Rotas públicas — leitura e registro de view
// Criação, edição e exclusão ficam no AdminVideoController
@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public ResponseEntity<List<VideoDTO>> getAllVideos() {
        return ResponseEntity.ok(videoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideoById(@PathVariable UUID id) {
        return ResponseEntity.ok(videoService.findById(id));
    }

    // Qualquer usuário autenticado registra view ao assistir
    @PatchMapping("/{id}/view")
    public ResponseEntity<Void> registerView(@PathVariable UUID id) {
        videoService.registerView(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/most-viewed")
    public ResponseEntity<List<VideoDTO>> getMostViewed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(videoService.getMostWatchedVideos(limit));
    }

    @GetMapping("/least-viewed")
    public ResponseEntity<List<VideoDTO>> getLeastViewed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(videoService.getLeastWatchedVideos(limit));
    }

    @GetMapping("/views-by-category")
    public ResponseEntity<Map<String, Long>> getViewsByCategory() {
        return ResponseEntity.ok(videoService.getTotalViewsByCategory());
    }
}