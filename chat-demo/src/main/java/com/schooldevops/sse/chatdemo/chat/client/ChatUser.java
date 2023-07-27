package com.schooldevops.sse.chatdemo.chat.client;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@Data
public class ChatUser {
    private String name;
    private String description;
}
