package com.dvFabricio.VidaLongaFlix.infra.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class ResourceExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResourceExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundExceptions.class)
    public ResponseEntity<StandardError> handleResourceNotFound(ResourceNotFoundExceptions e, HttpServletRequest request) {
        logger.error("Resource not found: {}", e.getMessage());
        HttpStatus status = HttpStatus.NOT_FOUND;
        var err = new StandardError(
                Instant.now(),
                status.value(),
                "Resource Not Found",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardError> handleDatabaseException(DatabaseException e, HttpServletRequest request) {
        logger.error("Database error: {}", e.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        var err = new StandardError(
                Instant.now(),
                status.value(),
                "Database Error",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        logger.error("Validation error: {}", e.getMessage());
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        var err = new ValidationError();
        err.setTimestamp(Instant.now());
        err.setStatus(status.value());
        err.setError("Validation Error");
        err.setMessage("Invalid fields. Please correct the errors and try again.");
        err.setPath(request.getRequestURI());

        for (FieldError f : e.getBindingResult().getFieldErrors()) {
            err.addError(f.getField(), f.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<OAuthCustomError> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        logger.error("Access forbidden: {}", e.getMessage());
        var err = new OAuthCustomError("Forbidden", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<OAuthCustomError> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        logger.error("Unauthorized access: {}", e.getMessage());
        var err = new OAuthCustomError("Unauthorized", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<OAuthCustomError> handleJwtException(JwtException e, HttpServletRequest request) {
        logger.error("JWT error: {}", e.getMessage());
        var err = new OAuthCustomError("JWT Error", "Invalid or expired token.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<StandardError> handleDuplicateResourceException(DuplicateResourceException e, HttpServletRequest request) {
        logger.error("Duplicate resource: {}", e.getMessage());
        HttpStatus status = HttpStatus.CONFLICT;
        var err = new StandardError(
                Instant.now(),
                status.value(),
                "Duplicate Resource",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGenericException(Exception e, HttpServletRequest request) {
        logger.error("Unexpected error: ", e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        var err = new StandardError(
                Instant.now(),
                status.value(),
                "Unexpected Error",
                "An unexpected error occurred. Please contact support if the issue persists.",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }
}
