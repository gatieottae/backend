package com.gatieottae.backend.common.exception;

public class BadRequestException extends RuntimeException {
    public final ErrorCode code;

    public BadRequestException(String message) {
        super(message);
        this.code = ErrorCode.BAD_REQUEST;
    }

    public BadRequestException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}