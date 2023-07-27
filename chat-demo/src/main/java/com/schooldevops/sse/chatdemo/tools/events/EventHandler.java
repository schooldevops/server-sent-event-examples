package com.schooldevops.sse.chatdemo.tools.events;

import com.schooldevops.sse.chatdemo.tools.client.Client;
import com.schooldevops.sse.chatdemo.tools.message.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


public interface EventHandler<T> {

    SseEmitter registerClient();
    void removeClient(Client<T> client);
    void broadcast(Message dto);
    void sendConnectedMessage(Client<T> client);
    void sendMessage(Client<T> client, Message dto);
}
