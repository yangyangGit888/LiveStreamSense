package com.yangyang.demo_01.douyin.dto;

import lombok.Data;

/**
 * IM 消息传输对象
 * 对应 content.js/background.js 发送的 JSON 结构
 */
@Data
public class ImMessageDto {
    /**
     * 消息方法名 (如 WebcastChatMessage)
     */
    private String method;

    /**
     * Base64 编码的 payload (Protobuf 字节)
     */
    private String payload;

    /**
     * 时间戳
     */
    private Long ts;
}
