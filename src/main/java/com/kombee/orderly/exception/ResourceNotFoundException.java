package com.kombee.orderly.exception;

/**
 * Use for "not found" cases so the API returns 404 instead of 400.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
