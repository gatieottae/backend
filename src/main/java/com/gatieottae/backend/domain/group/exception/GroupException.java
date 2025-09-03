package com.gatieottae.backend.domain.group.exception;

import lombok.Getter;

/**
 * 그룹 도메인 전용 예외
 */
@Getter
public class GroupException extends RuntimeException {

    private final GroupErrorCode errorCode;

    public GroupException(GroupErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}