package com.nextgen.bank.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int statusCode,
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<String> errors
) {
    public ApiError(int statusCode, String errorCode, String message, String path) {
        this(statusCode, errorCode, message, path, Instant.now(), null);
    }

    public ApiError(int statusCode, String errorCode, String message, String path, List<String> errors) {
        this(statusCode, errorCode, message, path, Instant.now(), errors);
    }
}
