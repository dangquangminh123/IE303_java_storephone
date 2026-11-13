package com.storephone.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("Email đã được sử dụng.");
    }

    public DuplicateEmailException(String message) {
        super(message);
    }
}
