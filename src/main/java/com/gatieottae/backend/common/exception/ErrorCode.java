package com.gatieottae.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // 401 / 403 (나중에 인증/인가 붙일 때 사용)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),

    // 409
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 username 입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 email 입니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 서버 상태와 충돌합니다."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    public final HttpStatus status;
    public final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}