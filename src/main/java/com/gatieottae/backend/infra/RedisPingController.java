package com.gatieottae.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/redis")
@RequiredArgsConstructor
public class RedisPingController {

    private final StringRedisTemplate srt;

    /**
     * 예: GET /internal/redis/ping
     * 동작: set("ping","pong"), get("ping") 반환
     */
    @GetMapping("/ping")
    public String ping() {
        srt.opsForValue().set("ping", "pong");
        return srt.opsForValue().get("ping");
    }
}