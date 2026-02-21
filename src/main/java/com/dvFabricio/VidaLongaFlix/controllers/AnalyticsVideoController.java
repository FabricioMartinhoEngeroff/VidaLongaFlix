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

    @GetMapping("/most-viewed")
    public ResponseEntity<List<VideoDTO>> getMostViewed(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(videoService.getMostWatchedVideos(limit));
    }

    @GetMapping("/least-viewed")
    public ResponseEntity<List<VideoDTO>> getLeastViewed(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(videoService.getLeastWatchedVideos(limit));
    }

    @GetMapping("/views-by-category")
    public ResponseEntity<Map<String, Long>> getViewsByCategory() {
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
