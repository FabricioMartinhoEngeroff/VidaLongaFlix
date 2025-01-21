package com.dvFabricio.VidaLongaFlix.infra.exception.comment;


public class RatingException extends RuntimeException {
    public RatingException(String message) {
        super(message);
    }

    public RatingException(String message, Throwable cause) {
        super(message, cause);
    }
}