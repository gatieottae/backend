package com.gatieottae.backend.api.me;

import com.gatieottae.backend.api.me.dto.SortOption;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorUtils {
    private CursorUtils(){}

    public static String encodeStart(SortOption sort, String isoDateOrNULL, long id){
        String raw = sort.param + "|" + (isoDateOrNULL == null ? "NULL" : isoDateOrNULL) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
    public static String encodeTitle(String title, long id){
        String raw = "titleAsc|" + (title == null ? "" : title) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Decoded decode(String cursor){
        try{
            String s = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] p = s.split("\\|", 3);
            if (p.length != 3) throw new IllegalArgumentException("bad parts");
            return new Decoded(p[0], p[1], Long.parseLong(p[2]));
        }catch(Exception e){
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    public record Decoded(String sortParam, String key, long id) {}
}