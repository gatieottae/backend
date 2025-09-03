package com.gatieottae.backend.api.group.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GroupJoinRequestDto {
    @NotBlank
    private String code;
}