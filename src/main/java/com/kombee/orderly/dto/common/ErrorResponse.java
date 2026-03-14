package com.kombee.orderly.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String error;
    private String message;
    private String path;
    private Instant timestamp;
    private Map<String, ?> details;

    public static ErrorResponse of(String error, String message, String path) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(String error, String message, String path, Map<String, ?> details) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .details(details)
                .build();
    }
}
