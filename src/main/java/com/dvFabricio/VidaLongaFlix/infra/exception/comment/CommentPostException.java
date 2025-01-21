package com.dvFabricio.VidaLongaFlix.infra.exception.comment;

public class CommentPostException extends RuntimeException {
    public CommentPostException(String message) {
        super(message);
    }

    public CommentPostException(String message, Throwable cause) {
        super(message, cause);
    }
}
