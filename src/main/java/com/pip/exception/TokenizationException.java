package com.pip.exception;

/**
 * Exceção lançada quando ocorre erro durante tokenização/detokenização
 */
public class TokenizationException extends RuntimeException {
    
    public TokenizationException(String message) {
        super(message);
    }
    
    public TokenizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
