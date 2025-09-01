package com.gatieottae.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /** 비밀번호 해싱을 위한 PasswordEncoder Bean */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Security 설정 */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API 개발 편의: CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/ping",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().permitAll()   // 지금은 전부 허용(개발용)
                )
                .httpBasic(Customizer.withDefaults()) // 필요시 기본 인증 테스트용
                .formLogin(form -> form.disable());   // 기본 로그인 페이지 비활성화
        return http.build();
    }
}