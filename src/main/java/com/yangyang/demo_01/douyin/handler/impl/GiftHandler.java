package com.yangyang.demo_01.douyin.handler.impl;

import com.google.protobuf.Message;
import com.yangyang.demo_01.douyin.handler.MessageHandler;
import com.yangyang.demo_01.douyin.proto.WebcastGiftMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 礼物消息处理器
 * 处理 WebcastGiftMessage 类型的消息
 */
@Slf4j
@Component
public class GiftHandler implements MessageHandler {
    
    @Override
    public String getMethod() {
        return "WebcastGiftMessage";
    }

    @Override
    public void handle(Message message) {
        if (message instanceof WebcastGiftMessage) {
            WebcastGiftMessage gift = (WebcastGiftMessage) message;
            
            String userName = "匿名用户";
            if (gift.hasUser()) {
                userName = gift.getUser().getNickName();
            }
            
            long count = gift.getComboCount();
            if (count == 0) {
                count = gift.getRepeatCount();
            }
            if (count == 0) {
                count = gift.getGroupCount(); // 可能是 group_count
            }

            log.info("[礼物] 用户[{}] 送出礼物 ID={} 连击={} 重复={} 组={} 价值(音浪)={}", 
                userName,
                gift.getGiftId(),
                gift.getComboCount(),
                gift.getRepeatCount(),
                gift.getGroupCount(),
                gift.getFanTicketCount());
        }
    }
}
