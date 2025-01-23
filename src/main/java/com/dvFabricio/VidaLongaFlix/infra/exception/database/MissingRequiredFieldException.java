package com.dvFabricio.VidaLongaFlix.infra.exception.database;

import lombok.Getter;


@Getter
public class MissingRequiredFieldException extends RuntimeException {

    private final String fieldName;

    public MissingRequiredFieldException(String fieldName, String additionalMessage) {
        super(buildMessage(fieldName, additionalMessage));
        this.fieldName = fieldName;
    }

    private static String buildMessage(String fieldName, String additionalMessage) {
        String baseMessage = "O campo obrigatório '" + fieldName + "' está ausente ou inválido.";
        return additionalMessage != null && !additionalMessage.isBlank()
                ? baseMessage + " " + additionalMessage
                : baseMessage;
    }

}

