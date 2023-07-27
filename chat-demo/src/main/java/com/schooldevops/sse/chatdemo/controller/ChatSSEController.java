package com.schooldevops.sse.chatdemo.controller;

import com.schooldevops.sse.chatdemo.chat.message.ChatMessageDto;
import com.schooldevops.sse.chatdemo.tools.events.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatSSEController {

    private final EventHandler eventHandler;

    @PostMapping("/message")
    public void sendMessage(@RequestBody ChatMessageDto message) {
        eventHandler.broadcast(message);
    }

    @GetMapping("/register")
    public SseEmitter register() {
        return eventHandler.registerClient();
    }
}
