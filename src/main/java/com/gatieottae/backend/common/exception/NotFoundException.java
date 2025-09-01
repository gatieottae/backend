package com.gatieottae.backend.common.exception;

public class NotFoundException extends RuntimeException {
    public final ErrorCode code;

    public NotFoundException(String message) {
        super(message);
        this.code = ErrorCode.NOT_FOUND;
    }

    public NotFoundException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}