package com.yangyang.demo_01.douyin.handler.impl;

import com.google.protobuf.Message;
import com.yangyang.demo_01.douyin.handler.MessageHandler;
import com.yangyang.demo_01.douyin.proto.WebcastChatMessage;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Component;

/**
 * 弹幕消息处理器
 * 处理 WebcastChatMessage 类型的消息
 */
@Slf4j
@Component
public class ChatHandler implements MessageHandler {
    
    @Override
    public String getMethod() {
        return "WebcastChatMessage";
    }

    @Override
    public void handle(Message message) {
        if (message instanceof WebcastChatMessage) {
            WebcastChatMessage chat = (WebcastChatMessage) message;
            String user = "匿名用户";
            if (chat.hasUser()) {
                user = chat.getUser().getNickName();
            }
            log.info("[弹幕] {} 说: {}", user, chat.getContent());
        }
    }
}
