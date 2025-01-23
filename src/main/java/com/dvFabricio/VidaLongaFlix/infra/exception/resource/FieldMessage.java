package com.dvFabricio.VidaLongaFlix.infra.exception.resource;

import lombok.Getter;

@Getter
public record FieldMessage(String fieldName, String message) {
}
