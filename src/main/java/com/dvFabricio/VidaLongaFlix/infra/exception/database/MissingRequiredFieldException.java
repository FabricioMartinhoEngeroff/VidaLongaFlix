package com.dvFabricio.VidaLongaFlix.infra.exception.database;

import lombok.Getter;

@Getter
public class MissingRequiredFieldException extends RuntimeException {

    private final String fieldName;


    public MissingRequiredFieldException(String fieldName, String additionalMessage) {
        super("O campo obrigatório '" + fieldName + "' está ausente ou inválido. " + additionalMessage);
        this.fieldName = fieldName;
    }

}

