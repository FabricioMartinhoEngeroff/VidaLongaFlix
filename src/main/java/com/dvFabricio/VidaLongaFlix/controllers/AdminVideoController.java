package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.services.MediaStorageService;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/admin/videos")
public class AdminVideoController {

    private final VideoService videoService;
    private final MediaStorageService mediaStorageService;

    public AdminVideoController(VideoService videoService, MediaStorageService mediaStorageService) {
        this.videoService = videoService;
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid VideoRequestDTO request) {
        videoService.create(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createWithFiles(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam UUID categoryId,
            @RequestParam(required = false) String recipe,
            @RequestParam(required = false) Double protein,
            @RequestParam(required = false) Double carbs,
            @RequestParam(required = false) Double fat,
            @RequestParam(required = false) Double fiber,
            @RequestParam(required = false) Double calories,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("coverFile") MultipartFile coverFile) {

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();

        String videoUrl = mediaStorageService.storeVideo(videoFile, baseUrl);
        String coverUrl = mediaStorageService.storeCover(coverFile, baseUrl);

        try {
            VideoRequestDTO request = new VideoRequestDTO(
                    title,
                    description,
                    videoUrl,
                    coverUrl,
                    categoryId,
                    recipe,
                    protein,
                    carbs,
                    fat,
                    fiber,
                    calories
            );

            videoService.create(request);
            return ResponseEntity.status(201).build();
        } catch (RuntimeException e) {
            mediaStorageService.deleteByPublicUrl(videoUrl);
            mediaStorageService.deleteByPublicUrl(coverUrl);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideoDTO> update(
            @PathVariable UUID id,
            @RequestBody @Valid VideoRequestDTO request) {
        videoService.update(id, request);
        return ResponseEntity.ok(videoService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        videoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
