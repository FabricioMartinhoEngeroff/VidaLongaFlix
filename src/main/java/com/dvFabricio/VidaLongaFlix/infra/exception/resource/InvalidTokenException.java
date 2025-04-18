package com.dvFabricio.VidaLongaFlix.infra.exception.resource;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

}