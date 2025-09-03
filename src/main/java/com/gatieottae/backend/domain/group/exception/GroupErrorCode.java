package com.gatieottae.backend.domain.group.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 그룹 관련 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum GroupErrorCode {

    GROUP_NAME_DUPLICATED(HttpStatus.CONFLICT, "동일한 이름의 그룹이 이미 존재합니다.");

    private final HttpStatus status;
    private final String message;
}