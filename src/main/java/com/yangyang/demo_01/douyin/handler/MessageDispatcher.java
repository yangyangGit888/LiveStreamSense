package com.yangyang.demo_01.douyin.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.yangyang.demo_01.douyin.proto.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息分发器
 * 根据消息类型 (method) 将 Protobuf 消息分发给对应的 Handler
 */
@Slf4j
@Component
public class MessageDispatcher {

    private final Map<String, MessageHandler> handlers = new HashMap<>();
    
    // 创建一个固定线程池来异步处理消息，避免阻塞 HTTP 接收线程
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 构造函数，自动注入所有 MessageHandler 实现类
     */
    public MessageDispatcher(List<MessageHandler> handlerList) {
        for (MessageHandler handler : handlerList) {
            handlers.put(handler.getMethod(), handler);
        }
    }

    /**
     * 异步分发消息
     * @param method 消息方法名 (如 WebcastChatMessage)
     * @param payload 解压后的消息体
     */
    public void dispatch(String method, byte[] payload) {
        MessageHandler handler = handlers.get(method);
        if (handler == null) {
            // log.debug("未找到 Handler: {}", method);
            return;
        }

        // 提交到线程池异步执行
        executor.submit(() -> {
            try {
                // 根据 method 解析 Protobuf 消息
                Message parsedMessage = parsePayload(method, payload);
                if (parsedMessage != null) {
                    // 记录解析后的消息类型
                    log.info("成功解析消息，method={}, 消息类型={}", method, parsedMessage.getDescriptorForType().getFullName());
                    // 调用 Handler 执行具体逻辑
                    handler.handle(parsedMessage);
                }
            } catch (Exception e) {
                log.error("分发消息异常，method={}", method, e);
            }
        });
    }

    /**
     * 根据 method 解析对应的 Protobuf 消息对象
     */
    private Message parsePayload(String method, byte[] payload) throws InvalidProtocolBufferException {
        switch (method) {
            case "WebcastChatMessage":
                return WebcastChatMessage.parseFrom(payload);
            case "WebcastGiftMessage":
                return WebcastGiftMessage.parseFrom(payload);
            case "WebcastMemberMessage":
                return WebcastMemberMessage.parseFrom(payload);
            case "WebcastLikeMessage":
                return WebcastLikeMessage.parseFrom(payload);
            case "WebcastRoomStatsMessage":
                return WebcastRoomStatsMessage.parseFrom(payload);
            case "WebcastRanklistHourEntranceMessage":
                return WebcastRanklistHourEntranceMessage.parseFrom(payload);
            default:
                // 对于未知的 method，返回 null，由调用方处理
                return null;
        }
    }
}
