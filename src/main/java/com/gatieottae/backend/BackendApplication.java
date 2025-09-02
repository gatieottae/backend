package com.gatieottae.backend;

import com.gatieottae.backend.config.KakaoOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        com.gatieottae.backend.config.KakaoOAuthProperties.class,
        com.gatieottae.backend.security.jwt.JwtProperties.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
