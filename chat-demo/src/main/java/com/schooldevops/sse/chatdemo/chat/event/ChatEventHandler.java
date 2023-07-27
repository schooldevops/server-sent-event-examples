package com.schooldevops.sse.chatdemo.chat.event;

import com.schooldevops.sse.chatdemo.chat.client.ChatUser;
import com.schooldevops.sse.chatdemo.chat.message.ChatMessageDto;
import com.schooldevops.sse.chatdemo.tools.client.Client;
import com.schooldevops.sse.chatdemo.tools.events.EventHandler;
import com.schooldevops.sse.chatdemo.tools.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event;

@Component
@Slf4j
public class ChatEventHandler implements EventHandler<ChatUser> {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    public static final long DEFAULT_TIMEOUT = Long.MAX_VALUE;
    private final Set<Client> registeredClients = new HashSet<>();

    @Override
    public SseEmitter registerClient() {
        var emitter = new SseEmitter(DEFAULT_TIMEOUT);
        var client = new Client<>(emitter, ChatUser.builder().name(UUID.randomUUID().toString()).description("User...").build());
        emitter.onCompletion(() -> registeredClients.remove(client));
        emitter.onError((err) -> removeClient(client));
        emitter.onTimeout(() -> removeClient(client));
        registeredClients.add(client);
        sendConnectedMessage(client);

        log.info("New client registered {}", client.clientInfo().getName());
        return emitter;
    }

    @Override
    public void removeClient(Client<ChatUser> client) {
        log.info("Error during communication. Unregister client {}", client.clientInfo().getName());
        registeredClients.remove(client);
    }

    @Override
    public void sendConnectedMessage(Client<ChatUser> client) {

        ChatMessageDto message = ChatMessageDto.builder()
                .eventType("welcome")
                .userName(client.clientInfo().getName())
                .message(String.format("Welcome {} come in", client.clientInfo().getName()))
                .dateTime(LocalDateTime.now())
                .build();
        sendMessage(client, message);
    }

    @Override
    public void broadcast(Message dto) {
        Set<Client> clients = Set.copyOf(registeredClients);
        for (Client<ChatUser> client: clients) {
            Message message = ChatMessageDto.builder()
                    .eventType("chat")
                    .userName(client.clientInfo().getName())
                    .message(dto.getMessage())
                    .dateTime(LocalDateTime.now())
                    .build();
            sendMessage(client, message);
        }
    }

    @Override
    public void sendMessage(Client<ChatUser> client, Message dto) {
        var sseEmitter = client.sseEmitter();
        try {
            log.info("Notify client " + client.clientInfo().getName());
            var eventId = ID_COUNTER.incrementAndGet();
            SseEmitter.SseEventBuilder eventBuilder = event().name(dto.eventType)
                    .id(String.valueOf(eventId))
                    .data(dto, MediaType.APPLICATION_JSON);
            sseEmitter.send(eventBuilder);
        } catch (IOException e) {
            sseEmitter.completeWithError(e);
        }
    }
}
