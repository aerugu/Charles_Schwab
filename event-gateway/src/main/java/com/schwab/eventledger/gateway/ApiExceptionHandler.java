package com.schwab.eventledger.gateway;

import com.schwab.eventledger.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        var messages = ex.getBindingResult().getFieldErrors().stream()
                .map(this::message)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", messages);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", List.of("Request body contains invalid JSON or unsupported values"));
    }

    @ExceptionHandler(EventNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(EventNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Not Found", List.of(ex.getMessage()));
    }

    @ExceptionHandler(AccountUnavailableException.class)
    ResponseEntity<ErrorResponse> accountUnavailable(AccountUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Account Service Unavailable", List.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", List.of(ex.getMessage()));
    }

    private String message(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String error, List<String> messages) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                messages,
                TraceContext.get()
        ));
    }
}
