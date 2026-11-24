package com.waste2value.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        int status,
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<ValidationError> validationErrors
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(false, status, errorCode, message, path, Instant.now(), null);
    }

    public static ErrorResponse ofValidation(int status, String errorCode, String message, String path, List<ValidationError> errors) {
        return new ErrorResponse(false, status, errorCode, message, path, Instant.now(), errors);
    }
}
