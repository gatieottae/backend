package com.gatieottae.backend.api.me.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class CursorPageResponse<T> {
    private final List<T> items;
    private final String nextCursor; // 더 없으면 null
}