package com.gatieottae.backend.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 공용 설정
 * - 타임아웃(커넥트/읽기/쓰기)
 * - 디버깅 시 용량 큰 응답을 위한 버퍼 확대 (필요 시)
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kakaoWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                                .build()
                )
                .build();
    }
}