package com.dvFabricio.VidaLongaFlix.infra.exception.authorization;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

