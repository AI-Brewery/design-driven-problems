package com.tejesh.urlshortener.exception;

import com.tejesh.urlshortener.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAlias(AliasAlreadyExistsException exception) {
        return error(HttpStatus.CONFLICT, "ALIAS_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler({UrlNotFoundException.class, ExpiredUrlException.class})
    public ResponseEntity<ErrorResponse> handleMissingUrl(RuntimeException exception) {
        String error = exception instanceof ExpiredUrlException ? "URL_EXPIRED" : "URL_NOT_FOUND";
        return error(HttpStatus.NOT_FOUND, error, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request is invalid");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation() {
        return error(HttpStatus.CONFLICT, "SHORT_CODE_CONFLICT", "The short code is already in use");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be completed");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), code, message));
    }
}