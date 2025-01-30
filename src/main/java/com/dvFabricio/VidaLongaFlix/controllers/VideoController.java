package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.DuplicateResourceException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        try {
            List<VideoDTO> videos = videoService.findAll();
            return ResponseEntity.ok(videos);
        } catch (DatabaseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoById(@PathVariable UUID id) {
        try {
            VideoDTO video = videoService.findById(id);
            return ResponseEntity.ok(video);
        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createVideo(@RequestBody @Valid VideoDTO videoDTO) {
        try {
            videoService.create(videoDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (MissingRequiredFieldException | DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DatabaseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVideo(@PathVariable UUID id, @RequestBody @Valid VideoDTO videoDTO) {
        try {
            videoService.update(id, videoDTO);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (MissingRequiredFieldException | DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (DatabaseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable UUID id) {
        try {
            videoService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundExceptions e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DatabaseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}