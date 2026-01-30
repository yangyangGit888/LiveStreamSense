package com.yangyang.demo_01.douyin.handler.impl;

import com.google.protobuf.Message;
import com.yangyang.demo_01.douyin.handler.MessageHandler;
import com.yangyang.demo_01.douyin.proto.WebcastMemberMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 进房消息处理器
 * 处理 WebcastMemberMessage 类型的消息
 */
@Slf4j
@Component
public class EnterRoomHandler implements MessageHandler {
    
    @Override
    public String getMethod() {
        return "WebcastMemberMessage";
    }

    @Override
    public void handle(Message message) {
        if (message instanceof WebcastMemberMessage) {
            WebcastMemberMessage member = (WebcastMemberMessage) message;
            
            String userName = "匿名用户";
            if (member.hasUser()) {
                userName = member.getUser().getNickName();
            }
            
            log.info("[进房] 用户[{}] 进入直播间 | 当前房间人数: {}", 
                userName,
                member.getMemberCount());
        }
    }
}
