package com.gatieottae.backend.api;

import com.gatieottae.backend.service.PingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PingController {

    private final PingService pingService;

    @GetMapping("/ping")
    public String ping() {
        return pingService.getPing();
    }
}