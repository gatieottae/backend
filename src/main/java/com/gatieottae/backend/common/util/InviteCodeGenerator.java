package com.gatieottae.backend.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 초대 코드 생성기
 * - 알파벳 대문자 + 숫자 조합
 * - 길이 기본값 8자리
 */

@Component
public class InviteCodeGenerator {

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    public static String generateDefault() {
        return generate(8); // 기본 8자리
    }
}