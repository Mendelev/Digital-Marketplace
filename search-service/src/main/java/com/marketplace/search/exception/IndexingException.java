package com.marketplace.search.exception;

/**
 * Exception thrown when indexing operations fail.
 */
public class IndexingException extends RuntimeException {

    public IndexingException(String message) {
        super(message);
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
