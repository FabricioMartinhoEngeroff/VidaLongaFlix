package com.dvFabricio.VidaLongaFlix.infra.exception.authorization;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}