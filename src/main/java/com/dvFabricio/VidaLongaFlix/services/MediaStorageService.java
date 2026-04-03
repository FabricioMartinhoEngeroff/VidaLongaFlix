package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.infra.exception.resource.FieldMessage;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class MediaStorageService {

    private final Path storageRoot;

    public MediaStorageService(
            @Value("${app.media.storage-path:/tmp/vidalongaflix-media}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(storageRoot.resolve("videos"));
            Files.createDirectories(storageRoot.resolve("covers"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize media storage: " + e.getMessage(), e);
        }
    }

    public String storeVideo(MultipartFile file, String publicBaseUrl) {
        return store(file, "videos", "video", publicBaseUrl);
    }

    public String storeCover(MultipartFile file, String publicBaseUrl) {
        return store(file, "covers", "cover", publicBaseUrl);
    }

    public void deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }

        int mediaIndex = publicUrl.indexOf("/media/");
        if (mediaIndex < 0) {
            return;
        }

        String relativePath = publicUrl.substring(mediaIndex + "/media/".length());
        Path target = storageRoot.resolve(relativePath).normalize();

        if (!target.startsWith(storageRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Avoid masking the original application error on cleanup.
        }
    }

    private String store(MultipartFile file, String directory, String fieldName, String publicBaseUrl) {
        validateFile(file, fieldName);

        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(directory).resolve(filename).normalize();

        if (!target.startsWith(storageRoot)) {
            throw new IllegalStateException("Invalid media storage path.");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store uploaded file: " + e.getMessage(), e);
        }

        return publicBaseUrl + "/media/" + directory + "/" + filename;
    }

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(List.of(
                    new FieldMessage(fieldName, "The uploaded file is required.")
            ));
        }

        String contentType = file.getContentType();
        boolean valid = switch (fieldName) {
            case "video" -> contentType != null && contentType.startsWith("video/");
            case "cover" -> contentType != null && contentType.startsWith("image/");
            default -> false;
        };

        if (!valid) {
            throw new ValidationException(List.of(
                    new FieldMessage(fieldName, "Send a valid " + fieldName + " file.")
            ));
        }
    }

    private String getExtension(String originalFilename) {
        String cleanFilename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int extensionIndex = cleanFilename.lastIndexOf('.');
        if (extensionIndex < 0) {
            return "";
        }
        return cleanFilename.substring(extensionIndex);
    }
}
