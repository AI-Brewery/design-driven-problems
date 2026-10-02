package com.tejesh.urlshortener.exception;

public class ExpiredUrlException extends RuntimeException {

    public ExpiredUrlException(String shortCode) {
        super("Short URL has expired: " + shortCode);
    }
}