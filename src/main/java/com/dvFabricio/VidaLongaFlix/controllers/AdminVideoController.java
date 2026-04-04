package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.video.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoRequestDTO;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.services.MediaStorageService;
import com.dvFabricio.VidaLongaFlix.services.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> create(@RequestBody @Valid VideoRequestDTO request) {
        videoService.create(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createMultipart(MultipartHttpServletRequest request) {
        videoService.create(buildCreateRequest(request));
        return ResponseEntity.status(201).build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VideoDTO> update(
            @PathVariable UUID id,
            @RequestBody @Valid VideoRequestDTO request) {
        videoService.update(id, request);
        return ResponseEntity.ok(videoService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoDTO> updateMultipart(
            @PathVariable UUID id,
            MultipartHttpServletRequest request) {
        videoService.update(id, buildUpdateRequest(request));
        return ResponseEntity.ok(videoService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        videoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private VideoRequestDTO buildCreateRequest(MultipartHttpServletRequest request) {
        return new VideoRequestDTO(
                requiredText(request, "title"),
                requiredText(request, "description"),
                resolveVideoUrl(request, true),
                resolveCoverUrl(request, true),
                requiredUuid(request, "categoryId"),
                optionalText(request, "recipe"),
                optionalDouble(request, "protein"),
                optionalDouble(request, "carbs"),
                optionalDouble(request, "fat"),
                optionalDouble(request, "fiber"),
                optionalDouble(request, "calories")
        );
    }

    private VideoRequestDTO buildUpdateRequest(MultipartHttpServletRequest request) {
        return new VideoRequestDTO(
                optionalText(request, "title"),
                optionalText(request, "description"),
                resolveVideoUrl(request, false),
                resolveCoverUrl(request, false),
                optionalUuid(request, "categoryId"),
                optionalText(request, "recipe"),
                optionalDouble(request, "protein"),
                optionalDouble(request, "carbs"),
                optionalDouble(request, "fat"),
                optionalDouble(request, "fiber"),
                optionalDouble(request, "calories")
        );
    }

    private String resolveVideoUrl(MultipartHttpServletRequest request, boolean required) {
        return resolveMediaField(
                request,
                "url",
                new String[]{"videoFile", "video", "url", "file"},
                "videos",
                required
        );
    }

    private String resolveCoverUrl(MultipartHttpServletRequest request, boolean required) {
        return resolveMediaField(
                request,
                "cover",
                new String[]{"coverFile", "cover", "thumbnail", "image"},
                "covers",
                required
        );
    }

    private String resolveMediaField(
            MultipartHttpServletRequest request,
            String fieldName,
            String[] fileFieldCandidates,
            String directoryName,
            boolean required
    ) {
        String value = optionalText(request, fieldName);
        if (value != null) {
            return value;
        }

        MultipartFile file = firstPresentFile(request, fileFieldCandidates);
        if (file != null) {
            return mediaStorageService.store(file, directoryName);
        }

        if (required) {
            throw new MissingRequiredFieldException(fieldName, "The field '" + fieldName + "' is required.");
        }
        return null;
    }

    private MultipartFile firstPresentFile(MultipartHttpServletRequest request, String[] candidates) {
        for (String candidate : candidates) {
            MultipartFile file = request.getFile(candidate);
            if (file != null && !file.isEmpty()) {
                return file;
            }
        }
        return null;
    }

    private String requiredText(MultipartHttpServletRequest request, String fieldName) {
        String value = optionalText(request, fieldName);
        if (value == null) {
            throw new MissingRequiredFieldException(fieldName, "The field '" + fieldName + "' is required.");
        }
        return value;
    }

    private String optionalText(MultipartHttpServletRequest request, String fieldName) {
        String value = request.getParameter(fieldName);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID requiredUuid(MultipartHttpServletRequest request, String fieldName) {
        String value = requiredText(request, fieldName);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID for field '" + fieldName + "'");
        }
    }

    private UUID optionalUuid(MultipartHttpServletRequest request, String fieldName) {
        String value = optionalText(request, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID for field '" + fieldName + "'");
        }
    }

    private Double optionalDouble(MultipartHttpServletRequest request, String fieldName) {
        String value = optionalText(request, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value for field '" + fieldName + "'");
        }
    }
}
