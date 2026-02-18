package com.mykulle.booking.system.shared.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(
            Exception ex,
            HttpServletRequest request
    ) {
        var message = switch (ex) {
            case MethodArgumentNotValidException methodArgumentNotValidException ->
                    formatValidationErrors(methodArgumentNotValidException);
            case BindException bindException ->
                    bindException.getAllErrors().stream()
                            .map(error -> error.getDefaultMessage() == null ? "Validation failed" : error.getDefaultMessage())
                            .sorted()
                            .reduce((left, right) -> left + "; " + right)
                            .orElse("Validation failed");
            case ConstraintViolationException constraintViolationException ->
                    constraintViolationException.getConstraintViolations().stream()
                            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                            .sorted()
                            .reduce((left, right) -> left + "; " + right)
                            .orElse("Validation failed");
            default -> "Validation failed";
        };

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, formatDataIntegrityMessage(ex), request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConversionFailedException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            Exception ex,
            HttpServletRequest request
    ) {
        var message = switch (ex) {
            case MethodArgumentTypeMismatchException mismatch -> {
                var expectedType = mismatch.getRequiredType() == null
                        ? "valid value"
                        : mismatch.getRequiredType().getSimpleName();
                yield "Invalid value '" + mismatch.getValue() + "' for parameter '" + mismatch.getName()
                        + "'. Expected " + expectedType;
            }
            case MissingServletRequestParameterException missing ->
                    "Missing required parameter: " + missing.getParameterName();
            default -> "Invalid request parameter";
        };

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    private String formatValidationErrors(MethodArgumentNotValidException ex) {
        if (!ex.getBindingResult().hasErrors()) {
            return "Validation failed";
        }

        return ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(error -> error.getField().toLowerCase(Locale.ROOT)))
                .map(error -> error.getField() + ": " +
                        (error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("Validation failed");
    }

    private String formatDataIntegrityMessage(DataIntegrityViolationException ex) {
        var message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (message != null) {
            var normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("room_location")) {
                return "Room location already exists";
            }
        }

        return "Data integrity violation";
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        var response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }
}
