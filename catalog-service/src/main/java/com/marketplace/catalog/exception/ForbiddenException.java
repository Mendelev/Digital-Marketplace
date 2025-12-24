package com.marketplace.catalog.exception;

import java.util.UUID;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
