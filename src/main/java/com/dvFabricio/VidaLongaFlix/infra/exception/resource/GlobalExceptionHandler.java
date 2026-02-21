package com.dvFabricio.VidaLongaFlix.infra.exception.resource;


import com.dvFabricio.VidaLongaFlix.domain.DTOs.ErrorResponse;
import com.dvFabricio.VidaLongaFlix.infra.exception.authorization.InvalidCredentialsException;
import com.dvFabricio.VidaLongaFlix.infra.exception.authorization.JwtException;
import com.dvFabricio.VidaLongaFlix.infra.exception.authorization.ForbiddenException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.time.LocalDateTime;


@ControllerAdvice
public class GlobalExceptionHandler {

    private StandardError buildStandardError(HttpStatus status, String error, String message, String path) {
        return new StandardError(Instant.now(), status.value(), error, message, path);
    }

    private ValidationError buildValidationError(HttpStatus status, String error, String message, String path) {
        return new ValidationError(Instant.now(), status.value(), error, message, path);
    }

    @ExceptionHandler(ResourceNotFoundExceptions.class)
    public ResponseEntity<StandardError> resourceNotFound(ResourceNotFoundExceptions e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<StandardError> duplicateResource(DuplicateResourceException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.CONFLICT, "Duplicate resource", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(MissingRequiredFieldException.class)
    public ResponseEntity<ValidationError> missingRequiredField(MissingRequiredFieldException e, HttpServletRequest request) {
        ValidationError err = buildValidationError(HttpStatus.BAD_REQUEST, "Missing required field", e.getMessage(), request.getRequestURI());
        err.addError(e.getFieldName(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(com.dvFabricio.VidaLongaFlix.infra.exception.resource.ValidationException.class)
    public ResponseEntity<ValidationError> handleValidationException(com.dvFabricio.VidaLongaFlix.infra.exception.resource.ValidationException e, HttpServletRequest request) {
        ValidationError err = buildValidationError(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation exception",
                e.getMessage(),
                request.getRequestURI()
        );

        e.getFieldMessages().forEach(fm -> err.addError(fm.fieldName(), fm.message()));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        ValidationError err = buildValidationError(HttpStatus.BAD_REQUEST, "Validation error", "Campos inválidos", request.getRequestURI());
        e.getBindingResult().getFieldErrors().forEach(f -> err.addError(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> accessDenied(AccessDeniedException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.FORBIDDEN, "Access denied", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardError> forbidden(ForbiddenException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<StandardError> jwtError(JwtException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.UNAUTHORIZED, "JWT error", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardError> databaseError(DatabaseException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardError> illegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.BAD_REQUEST, "Invalid argument", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> globalError(Exception e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<StandardError> handleInvalidToken(InvalidTokenException e, HttpServletRequest request) {
        StandardError err = buildStandardError(HttpStatus.BAD_REQUEST, "Token inválido", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<StandardError> invalidCredentials(
            InvalidCredentialsException e, HttpServletRequest request) {
        StandardError err = buildStandardError(
                HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }
}