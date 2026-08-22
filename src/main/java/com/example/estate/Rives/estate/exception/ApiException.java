package com.example.estate.Rives.estate.exception;

import org.springframework.http.HttpStatus;

// For business-rule rejections that need a specific HTTP status and a
// user-facing message but don't warrant their own dedicated exception type
// (e.g. duplicate signup fields). Keeps ad-hoc controller-level rejections
// flowing through GlobalExceptionHandler's structured response shape
// instead of each one hand-rolling a raw ResponseEntity.
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
