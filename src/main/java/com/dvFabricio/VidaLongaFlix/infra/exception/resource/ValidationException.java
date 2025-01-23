package com.dvFabricio.VidaLongaFlix.infra.exception.resource;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<FieldMessage> fieldMessages;

    public ValidationException(String message, List<FieldMessage> fieldMessages) {
        super(message);
        this.fieldMessages = fieldMessages;
    }

    public List<FieldMessage> getFieldMessages() {
        return fieldMessages;
    }
}
