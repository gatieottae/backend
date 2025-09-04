package com.gatieottae.backend.domain.group.exception;

import lombok.Getter;

/**
 * 그룹 관련 에러 코드 정의
 */
@Getter
public enum GroupErrorCode {
    GROUP_NAME_DUPLICATED("동일한 이름의 그룹이 이미 존재합니다."),
    INVALID_CODE("유효하지 않은 초대 코드입니다."),
    ALREADY_MEMBER("이미 그룹 멤버입니다."),
    GROUP_NOT_FOUND("존재하지 않는 그룹입니다."),
    NO_PERMISSION("이 그룹을 수정할 권한이 없습니다.");

    private final String message;
    GroupErrorCode(String message) { this.message = message; }
}