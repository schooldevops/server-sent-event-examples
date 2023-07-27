package com.schooldevops.sse.ssedemo.controller;

import com.schooldevops.sse.ssedemo.domain.Message;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class SSEController {
    private SseEmitter emitter;

    @GetMapping(path="/sse", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter createConnection() {
        emitter = new SseEmitter();
        return emitter;
    }

    @Scheduled(fixedRate = 2000)
    public void sendRandomMessage() {

        if (emitter == null) return;

        try {
            emitter.send("ScheduledId: " + UUID.randomUUID().toString());

        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private static final String[] WORDS = "The quick brown fox jumps over the lazy dog.".split(" ");

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    @GetMapping(path = "/words", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter getWords() {
        SseEmitter emitter = new SseEmitter();

        cachedThreadPool.execute(() -> {
            try {
                for (int i = 0; i < WORDS.length; i++) {
                    emitter.send(WORDS[i]);
                    TimeUnit.SECONDS.sleep(1);
                }

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
