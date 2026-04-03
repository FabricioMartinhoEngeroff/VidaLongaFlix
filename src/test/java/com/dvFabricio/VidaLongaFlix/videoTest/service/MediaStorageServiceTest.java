package com.dvFabricio.VidaLongaFlix.videoTest.service;

import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ValidationException;
import com.dvFabricio.VidaLongaFlix.services.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreVideoAndReturnPublicUrl() throws IOException {
        MediaStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
                "videoFile", "video.mp4", "video/mp4", "video-content".getBytes());

        String publicUrl = service.storeVideo(file, "https://vidalongaflix.com/api");

        Path storedFile = tempDir.resolve("videos").resolve(extractFilename(publicUrl));

        assertAll(
                () -> assertTrue(publicUrl.startsWith("https://vidalongaflix.com/api/media/videos/")),
                () -> assertTrue(Files.exists(storedFile)),
                () -> assertEquals("video-content", Files.readString(storedFile))
        );
    }

    @Test
    void shouldRejectInvalidCoverContentType() {
        MediaStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
                "coverFile", "cover.txt", "text/plain", "not-an-image".getBytes());

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.storeCover(file, "https://vidalongaflix.com/api"));

        assertEquals("cover", exception.getFieldMessages().get(0).fieldName());
    }

    @Test
    void shouldDeleteOnlyManagedFiles() throws IOException {
        MediaStorageService service = createService();
        MockMultipartFile file = new MockMultipartFile(
                "videoFile", "video.mp4", "video/mp4", "video-content".getBytes());

        String publicUrl = service.storeVideo(file, "https://vidalongaflix.com/api");
        Path storedFile = tempDir.resolve("videos").resolve(extractFilename(publicUrl));

        service.deleteByPublicUrl(publicUrl);
        service.deleteByPublicUrl("https://example.com/not-managed/file.mp4");

        assertFalse(Files.exists(storedFile));
    }

    private MediaStorageService createService() {
        MediaStorageService service = new MediaStorageService(tempDir.toString());
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }

    private String extractFilename(String publicUrl) {
        return publicUrl.substring(publicUrl.lastIndexOf('/') + 1);
    }

    private static void assertAll(ThrowingAssertion... assertions) throws IOException {
        for (ThrowingAssertion assertion : assertions) {
            assertion.run();
        }
    }

    @FunctionalInterface
    private interface ThrowingAssertion {
        void run() throws IOException;
    }
}
