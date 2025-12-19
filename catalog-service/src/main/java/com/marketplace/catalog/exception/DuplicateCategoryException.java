package com.marketplace.catalog.exception;

public class DuplicateCategoryException extends RuntimeException {
    public DuplicateCategoryException(String name) {
        super("Category already exists with name: " + name);
    }
}
