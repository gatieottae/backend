package com.gatieottae.backend.api.me.dto;

import java.util.Locale;

public enum SortOption {
    START_ASC("startAsc"),
    START_DESC("startDesc"),
    TITLE_ASC("titleAsc");

    public final String param;
    SortOption(String p){ this.param = p; }

    public static SortOption from(String v){
        if (v == null) return START_ASC;
        String s = v.toLowerCase(Locale.ROOT).trim();
        for (SortOption o : values()) if (o.param.equals(s)) return o;
        return START_ASC;
    }
}