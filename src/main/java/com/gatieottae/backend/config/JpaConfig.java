package com.gatieottae.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // createdAt, updatedAt 자동 세팅
public class JpaConfig {
}