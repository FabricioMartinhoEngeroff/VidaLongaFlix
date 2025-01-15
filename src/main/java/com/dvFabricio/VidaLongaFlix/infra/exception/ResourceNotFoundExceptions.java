package com.dvFabricio.VidaLongaFlix.infra.exception;

public class ResourceNotFoundExceptions extends RuntimeException {
    public ResourceNotFoundExceptions(String message) {
        super(message);
    }
}
