// background.js - Service Worker
// 职责：接收 content 数据，维护队列，可靠传输到 Java 后端

const QUEUE = [];
const BATCH_SIZE = 100; // 每次 fetch 最大条数
const FLUSH_INTERVAL = 200; // ms
const MAX_QUEUE_SIZE = 10000; // 防止后台队列无限膨胀
const BACKEND_URL = "http://127.0.0.1:8080/api/douyin/im"; // 新接口地址

console.log("[Background] IM Service Worker Started");

// 1. 接收消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === "IM_BATCH" && message.payload) {
        if (QUEUE.length < MAX_QUEUE_SIZE) {
            if (Array.isArray(message.payload)) {
                QUEUE.push(...message.payload);
            }
        } else {
            // 队列满，丢弃最旧的 (模拟环形缓冲，或者直接丢弃新的？通常丢弃旧的保活)
            // 这里简单策略：丢弃新来的，保护已有的；或者 splice 掉头部的。
            // 为了实时性，最好丢弃旧的。
            const overflow = QUEUE.length + message.payload.length - MAX_QUEUE_SIZE;
            if (overflow > 0) {
                QUEUE.splice(0, overflow);
            }
            QUEUE.push(...message.payload);
        }
    }
    return true; // 保持通道
});

// 2. 串行发送逻辑
let isFetching = false;

setInterval(async () => {
    if (QUEUE.length === 0 || isFetching) return;

    isFetching = true;

    // 取出一批
    const batch = QUEUE.splice(0, BATCH_SIZE);

    try {
        const response = await fetch(BACKEND_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(batch)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        // 成功，无需操作

    } catch (e) {
        console.error("[Background] Fetch failed, requeueing...", e);
        // 失败，放回队列头部 (保证顺序)
        QUEUE.unshift(...batch);
        
        // 如果队列太长，还是得丢弃一些，防止死循环导致内存溢出
        if (QUEUE.length > MAX_QUEUE_SIZE) {
            QUEUE.splice(MAX_QUEUE_SIZE); // 截断尾部
        }
    } finally {
        isFetching = false;
    }

}, FLUSH_INTERVAL);
