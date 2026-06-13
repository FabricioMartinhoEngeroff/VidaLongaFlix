package com.dvFabricio.VidaLongaFlix.infra.exception.resource;

public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}