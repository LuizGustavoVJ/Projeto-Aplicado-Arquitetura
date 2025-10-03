package com.pip.exception;

/**
 * Exceção lançada quando ocorre erro relacionado a API Keys
 */
public class ApiKeyException extends RuntimeException {
    
    public ApiKeyException(String message) {
        super(message);
    }
    
    public ApiKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
