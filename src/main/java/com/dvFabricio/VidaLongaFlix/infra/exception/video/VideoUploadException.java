package com.dvFabricio.VidaLongaFlix.infra.exception.video;

public class VideoUploadException extends RuntimeException {

    public VideoUploadException(String message) {
        super(message);
    }

    public VideoUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}