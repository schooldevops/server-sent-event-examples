package com.schooldevops.sse.chatdemo.tools.message;

public interface Message {

    String eventType = "all";

    String getMessageSender();
    String getMessage();
    String getDate();
    String getTime();

}
