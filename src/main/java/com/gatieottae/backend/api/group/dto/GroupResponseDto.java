package com.gatieottae.backend.api.group.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 그룹 생성/조회 응답 DTO
 * - API 응답에서 노출되는 필드만 포함
 */
@Getter
@Builder
public class GroupResponseDto {

    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private Instant inviteExpiresAt;
}