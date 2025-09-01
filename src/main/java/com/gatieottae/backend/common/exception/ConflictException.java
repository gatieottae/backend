package com.gatieottae.backend.common.exception;

public class ConflictException extends RuntimeException {
    public final ErrorCode code;

    public ConflictException(String message) {
        super(message);
        this.code = ErrorCode.CONFLICT;
    }

    public ConflictException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}