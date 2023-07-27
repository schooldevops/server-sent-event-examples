package com.schooldevops.sse.chatdemo.chat.message;

import com.schooldevops.sse.chatdemo.tools.message.Message;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Builder
@Getter
@Setter
public class ChatMessageDto implements Message {
    private String eventType;
    private String userName;
    private String message;
    private LocalDateTime dateTime;

    @Override
    public String getMessageSender() {
        return userName;
    }

    @Override
    public String getDate() {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String getTime() {
        return dateTime.format(DateTimeFormatter.ISO_TIME);
    }
}
