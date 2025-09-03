package com.gatieottae.backend.domain.group.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 그룹 관련 에러 코드 정의
 */
@Getter
public enum GroupErrorCode {
    GROUP_NAME_DUPLICATED("동일한 이름의 그룹이 이미 존재합니다."),
    INVALID_CODE("유효하지 않은 초대 코드입니다."),
    ALREADY_MEMBER("이미 그룹 멤버입니다.");

    private final String message;
    GroupErrorCode(String message) { this.message = message; }
}