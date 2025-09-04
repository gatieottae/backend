package com.gatieottae.backend.api.group.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class GroupDetailResponseDto {
    Long id;
    String name;
    String description;
    String destination;
    LocalDate startDate;
    LocalDate endDate;
    Long ownerId;
    int memberCount;
    List<MemberDto> members;

    @Value
    @Builder
    public static class MemberDto {
        Long id;
        String displayName; // nickname 우선, 없으면 name
        String role;        // OWNER / MEMBER
    }
}