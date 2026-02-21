package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @PostMapping
    public ResponseEntity<Void> createVideo(@RequestBody @Valid VideoRequestDTO request) {
        videoService.create(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateVideo(@PathVariable UUID id,
                                            @RequestBody @Valid VideoRequestDTO request) {
        videoService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) {
        videoService.delete(id);
        return ResponseEntity.noContent().build();
    }

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
