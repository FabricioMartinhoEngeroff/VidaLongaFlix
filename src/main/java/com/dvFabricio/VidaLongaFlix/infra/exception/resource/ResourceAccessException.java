package com.dvFabricio.VidaLongaFlix.infra.exception.resource;

public class ResourceAccessException extends RuntimeException {

    public ResourceAccessException(String message) {
        super(message);
    }
}
