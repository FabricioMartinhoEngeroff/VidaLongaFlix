package com.dvFabricio.VidaLongaFlix.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Armazena arquivos de mídia (vídeos e capas).
 *
 * Modo S3 (produção): ativo quando AWS_S3_BUCKET está definido.
 *   - faz upload direto para o bucket S3
 *   - retorna URL pública: CloudFront (CDN_BASE_URL) ou S3 direto
 *
 * Modo local (dev/fallback): arquivos em media.storage.path, servidos em /media/**.
 */
@Service
public class MediaStorageService {

    private final String bucketName;
    private final String s3Region;
    private final String cdnBaseUrl;
    private final Path localStorageRoot;

    private S3Client s3Client;

    public MediaStorageService(
            @Value("${aws.s3.bucket:}") String bucketName,
            @Value("${aws.s3.region:us-east-2}") String s3Region,
            @Value("${aws.cdn.base-url:}") String cdnBaseUrl,
            @Value("${media.storage.path:${java.io.tmpdir}/vidalongaflix-media}") String localPath) {
        this.bucketName = bucketName.trim();
        this.s3Region = s3Region.trim();
        this.cdnBaseUrl = cdnBaseUrl.trim();
        this.localStorageRoot = Paths.get(localPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        if (isS3Enabled()) {
            this.s3Client = S3Client.builder()
                    .region(Region.of(s3Region))
                    .build();
        } else {
            Files.createDirectories(localStorageRoot.resolve("videos"));
            Files.createDirectories(localStorageRoot.resolve("covers"));
        }
    }

    public String store(MultipartFile file, String directoryName) {
        if (isS3Enabled()) {
            return storeInS3(file, directoryName);
        }
        return storeLocally(file, directoryName);
    }

    private boolean isS3Enabled() {
        return !bucketName.isBlank();
    }

    // ── S3 ──────────────────────────────────────────────────────────────────

    private String storeInS3(MultipartFile file, String directoryName) {
        String ext = extractExtension(file.getOriginalFilename());
        String key = sanitizeSegment(directoryName) + "/" + UUID.randomUUID() + ext;

        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(in, file.getSize())
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload to S3: " + e.getMessage(), e);
        }

        if (!cdnBaseUrl.isBlank()) {
            return cdnBaseUrl.replaceAll("/+$", "") + "/" + key;
        }
        return "https://" + bucketName + ".s3." + s3Region + ".amazonaws.com/" + key;
    }

    // ── Local (dev / fallback) ───────────────────────────────────────────────

    private String storeLocally(MultipartFile file, String directoryName) {
        try {
            String safeDir = sanitizeSegment(directoryName);
            Path targetDir = localStorageRoot.resolve(safeDir);
            Files.createDirectories(targetDir);

            String fileName = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
            Path target = targetDir.resolve(fileName).normalize();

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/media/")
                    .path(safeDir)
                    .path("/")
                    .path(fileName)
                    .toUriString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file locally: " + e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) return "";
        String ext = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(ext)) return "";
        String sanitized = ext.replaceAll("[^A-Za-z0-9]", "");
        return sanitized.isEmpty() ? "" : "." + sanitized;
    }

    private String sanitizeSegment(String value) {
        String sanitized = value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "");
        return sanitized.isBlank() ? "media" : sanitized;
    }
}