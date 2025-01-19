package com.dvFabricio.VidaLongaFlix.infra.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {
    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

}
