package com.dvFabricio.VidaLongaFlix.infra.exception.authorization;

import com.dvFabricio.VidaLongaFlix.infra.exception.resource.StandardError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class AccessDeniedExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> handleAccessDeniedException(AccessDeniedException ex) {
        StandardError error = new StandardError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                ex.getMessage(),
                "Access Denied"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}