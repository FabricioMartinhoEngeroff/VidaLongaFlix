package com.dvFabricio.VidaLongaFlix.infra.exception.video;

public class VideoAccessException extends RuntimeException {

    public VideoAccessException(String message) {
        super(message);
    }

    public VideoAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
