package com.gatieottae.backend.api.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeRequestDto(
        @NotBlank
        @Size(max = 100)
        String name
) {}