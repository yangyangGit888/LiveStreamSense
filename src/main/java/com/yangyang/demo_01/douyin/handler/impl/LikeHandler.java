package com.yangyang.demo_01.douyin.handler.impl;

import com.google.protobuf.Message;
import com.yangyang.demo_01.douyin.handler.MessageHandler;
import com.yangyang.demo_01.douyin.proto.WebcastLikeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 点赞消息处理器
 * 处理 WebcastLikeMessage 类型的消息
 */
@Slf4j
@Component
public class LikeHandler implements MessageHandler {
    
    @Override
    public String getMethod() {
        return "WebcastLikeMessage";
    }

    @Override
    public void handle(Message message) {
        if (message instanceof WebcastLikeMessage) {
            WebcastLikeMessage like = (WebcastLikeMessage) message;
            
            String userName = "匿名用户";
            if (like.hasUser()) {
                userName = like.getUser().getNickName();
            }
            
            log.info("[点赞] 用户[{}] 点赞了 count={} total={}", 
                userName, 
                like.getCount(), 
                like.getTotal());
        }
    }
}
