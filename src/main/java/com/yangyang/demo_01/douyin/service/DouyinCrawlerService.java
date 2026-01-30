package com.yangyang.demo_01.douyin.service;

import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.FileSystemUtils;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抖音直播爬虫服务
 * 负责管理 Playwright 浏览器实例、加载 Chrome Extension、注入 Hook 脚本以及导航到直播间
 */
@Slf4j
@Service
public class DouyinCrawlerService {

    private Playwright playwright;
    private BrowserContext context;
    
    // 当前正在采集的直播间页面
    private Page currentRoomPage;

    // 临时存放 Extension 的目录
    private Path extensionPath;

    /**
     * 步骤 1: 启动浏览器 (加载 Extension) 并等待用户登录
     * 
     * @return 启动结果描述
     */
    public String launchBrowser() {
        try {
            // 如果已有实例，先清理
            if (context != null) {
                log.info("检测到旧浏览器实例，正在关闭...");
                close();
            }

            log.info("正在准备 Chrome Extension...");
            prepareExtension();
            String extPathStr = extensionPath.toAbsolutePath().toString();
            log.info("Extension 路径: {}", extPathStr);

            log.info("正在启动 Playwright (Persistent Context Mode)...");
            playwright = Playwright.create();

            // 为了加载 Extension，必须使用 launchPersistentContext
            // 且必须指定 user-data-dir
            Path userDataDir = Files.createTempDirectory("playwright-user-data-");
            
            List<String> args = new ArrayList<>();
            args.add("--disable-blink-features=AutomationControlled"); // 去除自动化特征
            args.add("--disable-extensions-except=" + extPathStr);     // 加载指定插件
            args.add("--load-extension=" + extPathStr);                // 加载指定插件

            context = playwright.chromium().launchPersistentContext(userDataDir, new BrowserType.LaunchPersistentContextOptions()
                    .setChannel("msedge") // 使用 Edge
                    .setHeadless(false)   // 显示界面
                    .setArgs(args)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0")
            );

            // 打开主页
            if (context.pages().isEmpty()) {
                context.newPage();
            }

            // 读取并注入 Hook 脚本 (page.js)
            // 这一步非常关键：必须在页面加载前注入，才能拦截到早期的 WebSocket 消息
            try {
                File pageJsFile = ResourceUtils.getFile("classpath:extension/page.js");
                String pageJsContent = Files.readString(pageJsFile.toPath());
                context.addInitScript(pageJsContent);
                log.info("Hook 脚本 (page.js) 已注入到浏览器上下文");
            } catch (Exception e) {
                log.error("注入 Hook 脚本失败: 找不到 extension/page.js 文件", e);
            }

            Page page = context.pages().get(0);
            page.navigate("https://www.douyin.com/");
            
            // 移除自动关闭逻辑，允许用户手动多 Tab 操作
            log.info("浏览器已启动 (已加载 CSP Bypass 插件)。您可以手动扫码登录或开启新 Tab。");

            return "浏览器启动成功！您可以手动操作，支持多 Tab。";

        } catch (Exception e) {
            log.error("启动浏览器失败", e);
            close();
            return "启动失败: " + e.getMessage();
        }
    }

    /**
     * 准备 Extension 文件
     * 将 src/main/resources/extension 下的文件复制到临时目录
     */
    private void prepareExtension() throws IOException {
        // 创建临时目录
        extensionPath = Files.createTempDirectory("douyin-hook-ext-");
        
        // 获取源文件 (假设在 resources/extension)
        File sourceDir = ResourceUtils.getFile("classpath:extension");
        
        if (sourceDir.exists() && sourceDir.isDirectory()) {
            FileSystemUtils.copyRecursively(sourceDir, extensionPath.toFile());
        } else {
            throw new IOException("找不到 Extension 源文件: " + sourceDir.getAbsolutePath());
        }
    }

    /**
     * 步骤 2: 在新标签页打开直播间并注入 Hook
     * 如果之前已经通过此接口打开了直播间，会关闭旧的直播间 Tab，保留浏览器实例。
     * 
     * @param roomId 直播间 ID (数字 ID)
     * @return 操作结果
     */
    public String openLiveRoom(String roomId) {
        if (context == null) {
            return "错误：浏览器未启动，请先调用 /launch 接口启动浏览器。";
        }

        try {
            // 如果已有由程序控制的直播间页面，先关闭它 (避免 Tab 越开越多)
            if (currentRoomPage != null && !currentRoomPage.isClosed()) {
                log.info("正在关闭旧直播间 Tab...");
                currentRoomPage.close();
            }

            // 新建页面进入直播间
            currentRoomPage = context.newPage();

            // 屏蔽无关资源 (节省流量)
            currentRoomPage.route("**/*.{png,jpg,jpeg,gif,svg,mp4,avi,mp3,ttf,woff,woff2}", route -> route.abort());

            log.info("正在进入直播间: {}", roomId);
            currentRoomPage.navigate("https://live.douyin.com/" + roomId);

            return "已打开直播间: " + roomId + " (旧直播间 Tab 已关闭，浏览器保持运行)";

        } catch (Exception e) {
            log.error("打开直播间失败", e);
            return "打开直播间失败: " + e.getMessage();
        }
    }

    @PreDestroy
    public void close() {
        if (context != null) {
            try {
                // 关闭整个 Context (浏览器窗口)
                context.close();
                playwright.close();
            } catch (Exception e) {
                log.warn("关闭浏览器异常", e);
            } finally {
                context = null;
                playwright = null;
                currentRoomPage = null;
            }
        }
        
        // 清理临时 Extension 目录
        if (extensionPath != null) {
            try {
                FileSystemUtils.deleteRecursively(extensionPath);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
