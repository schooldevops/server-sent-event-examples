package com.schooldevops.sse.chatdemo.tools.client;

import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record Client<T>(SseEmitter sseEmitter, T clientInfo) {
}
