package com.yangyang.demo_01.douyin.handler;

import com.google.protobuf.Message;

/**
 * 消息处理器接口
 * 所有具体的业务消息处理类（如弹幕、礼物、点赞）都必须实现此接口
 */
public interface MessageHandler {
    /**
     * 获取当前处理器支持的消息方法名
     * 例如："WebcastChatMessage"
     * 
     * @return 消息方法名
     */
    String getMethod();
    
    /**
     * 处理具体的消息
     * 
     * @param message 解析后的 Protobuf 消息对象
     */
    void handle(Message message);
}
