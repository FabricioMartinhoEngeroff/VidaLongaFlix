package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/videos")
public class AnalyticsVideoController {

    private final VideoService videoService;

    public AnalyticsVideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/mais-assistidos")
    public ResponseEntity<?> getMostWatchedVideos(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<VideoDTO> videos = videoService.getMostWatchedVideos(limit);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao recuperar os vídeos mais assistidos: " + e.getMessage());
        }
    }

    @GetMapping("/menos-assistidos")
    public ResponseEntity<?> getLeastWatchedVideos(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<VideoDTO> videos = videoService.getLeastWatchedVideos(limit);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao recuperar os vídeos menos assistidos: " + e.getMessage());
        }
    }

    @GetMapping("/visualizacoes-por-categoria")
    public ResponseEntity<?> getTotalViewsByCategory() {
        try {
            Map<String, Long> viewsByCategory = videoService.getTotalViewsByCategory();
            return ResponseEntity.ok(viewsByCategory);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao recuperar visualizações por categoria: " + e.getMessage());
        }
    }

    @GetMapping("/{videoId}/tempo-medio-assistido")
    public ResponseEntity<?> getAverageWatchTime(@PathVariable UUID videoId) {
        try {
            double averageWatchTime = videoService.getAverageWatchTime(videoId);
            return ResponseEntity.ok(averageWatchTime);
        } catch (ResourceNotFoundExceptions e) {
            throw new ResourceNotFoundExceptions("Vídeo com ID " + videoId + " não encontrado: " + e.getMessage());
        } catch (Exception e) {
            throw new DatabaseException("Erro ao recuperar o tempo médio assistido para o vídeo com ID " + videoId + ": " + e.getMessage());
        }
    }

    @GetMapping("/mais-comentados")
    public ResponseEntity<?> getVideosWithMostComments(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<VideoDTO> videos = videoService.getVideosWithMostComments(limit);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao recuperar os vídeos mais comentados: " + e.getMessage());
        }
    }
}
