package com.gatieottae.backend.config;

import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtAccessDeniedHandler;
import com.gatieottae.backend.security.jwt.JwtAuthenticationEntryPoint;
import com.gatieottae.backend.security.jwt.JwtAuthenticationFilter;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);

        // ✅ CORS 적용
        http.cors(Customizer.withDefaults());

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/refresh"
                ).permitAll()
                .requestMatchers(
                        "/api/auth/kakao/login-url",
                        "/api/auth/kakao/callback",
                        "/api/ping",
                        "/actuator/health",
                        "/actuator/info",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, memberRepository),
                UsernamePasswordAuthenticationFilter.class);

        http.httpBasic(AbstractHttpConfigurer::disable); // JWT라서 비활성 권장
        http.formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // ✅ CORS 설정 Bean 추가
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
