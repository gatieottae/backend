package com.gatieottae.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PingService {
    public String getPing() {
        return "pong";
    }
}