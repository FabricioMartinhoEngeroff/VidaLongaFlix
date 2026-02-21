package com.dvFabricio.VidaLongaFlix.infra.exception.authorization;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
