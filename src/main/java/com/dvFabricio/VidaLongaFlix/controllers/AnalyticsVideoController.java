package com.dvFabricio.VidaLongaFlix.controllers;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/videos")
@RequiredArgsConstructor
public class AnalyticsVideoController {

    private final VideoService videoService;

    @GetMapping("/mais-assistidos")
    public ResponseEntity<List<VideoDTO>> getMostWatchedVideos(@RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(videoService.getMostWatchedVideos(limit));
    }

    @GetMapping("/menos-assistidos")
    public ResponseEntity<List<VideoDTO>> getLeastWatchedVideos(@RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(videoService.getLeastWatchedVideos(limit));
    }

    @GetMapping("/visualizacoes-por-categoria")
    public ResponseEntity<Map<String, Long>> getTotalViewsByCategory() {
        return ResponseEntity.ok(videoService.getTotalViewsByCategory());
    }

    @GetMapping("/{videoId}/tempo-medio-assistido")
    public ResponseEntity<Double> getAverageWatchTime(@PathVariable UUID videoId) {
        return ResponseEntity.ok(videoService.getAverageWatchTime(videoId));
    }

    @GetMapping("/mais-comentados")
    public ResponseEntity<List<VideoDTO>> getVideosWithMostComments(@RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(videoService.getVideosWithMostComments(limit));
    }
}
