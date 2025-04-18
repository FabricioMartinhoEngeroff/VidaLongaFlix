package com.dvFabricio.VidaLongaFlix.infra.exception.resource;

import java.util.List;

public final class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<FieldMessage> fieldMessages;

    public ValidationException(String message, List<FieldMessage> fieldMessages) {
        super(message);
        this.fieldMessages = fieldMessages;
    }

    public ValidationException(List<FieldMessage> fieldMessages) {
        this("Erro de validação nos campos", fieldMessages);
    }

    public List<FieldMessage> getFieldMessages() {
        return fieldMessages;
    }
}
