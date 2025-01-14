package com.dvFabricio.VidaLongaFlix.infra.exception;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
}
