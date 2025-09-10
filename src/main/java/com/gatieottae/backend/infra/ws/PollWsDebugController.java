package com.gatieottae.backend.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * /app/polls/{id}/ping 로 메시지를 보내면
 * /topic/polls/{id} 로 에코.
 * (로컬 디버그용)
 */
@Controller
@RequiredArgsConstructor
public class PollWsDebugController {

    private final SimpMessagingTemplate template;

    @MessageMapping("/polls/{id}/ping")
    public void ping(String body, @org.springframework.messaging.handler.annotation.DestinationVariable long id) {
        template.convertAndSend("/topic/polls/" + id, body);
    }
}