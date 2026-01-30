package com.yangyang.demo_01.douyin.controller;

import com.yangyang.demo_01.douyin.dto.ImMessageDto;
import com.yangyang.demo_01.douyin.handler.MessageDispatcher;
import com.yangyang.demo_01.douyin.service.DouyinCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

/**
 * 抖音数据采集控制器
 * 提供浏览器管理接口和数据接收接口
 */
@Slf4j
@RestController
@RequestMapping("/api/douyin")
@RequiredArgsConstructor
public class DouyinDataController {

    private final DouyinCrawlerService crawlerService;
    private final MessageDispatcher messageDispatcher;

    /**
     * 第一步：启动浏览器进行登录
     * 访问此接口启动 Edge 浏览器，请在 1 分钟内完成扫码登录
     * 
     * @return 启动状态消息
     */
    @GetMapping("/launch")
    public String launchBrowser() {
        log.info("收到请求：启动浏览器...");
        return crawlerService.launchBrowser();
    }

    /**
     * 第二步：在新标签页打开直播间 (登录完成后调用)
     * 
     * @param roomId 直播间 ID
     * @return 操作结果
     */
    @GetMapping("/room/{roomId}")
    public String openRoom(@PathVariable String roomId) {
        log.info("收到请求：打开直播间 {}", roomId);
        return crawlerService.openLiveRoom(roomId);
    }

    /**
     * 新版 IM 消息接收接口
     * 接收 extension 转发的已解码消息 (JSON 数组)
     * 
     * @param messages 消息列表
     */
    @PostMapping("/im")
    public void receiveImMessages(@RequestBody List<ImMessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        log.info("收到 IM 消息批次，数量: {}", messages.size());

        for (ImMessageDto msg : messages) {
            try {
                // Base64 解码 payload
                byte[] payload = Base64.getDecoder().decode(msg.getPayload());
                
                // 直接分发给对应的 Handler
                messageDispatcher.dispatch(msg.getMethod(), payload);
                
            } catch (Exception e) {
                log.error("处理 IM 消息失败: method={}", msg.getMethod(), e);
            }
        }
    }
}
