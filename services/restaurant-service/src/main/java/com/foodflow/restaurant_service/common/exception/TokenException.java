package com.foodflow.restaurant_service.common.exception;

public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
}