// 抖音直播 WebSocket 采集 Hook
// 运行在 document-start
// 修改版：通过 postMessage 转发给 Chrome Extension Content Script，绕过 CSP

(function () {
    console.log("🔥 Douyin Hook Loaded! (CSP Bypass Mode)");

    const OLD_WS = window.WebSocket;
    const MSG_QUEUE = [];
    const FLUSH_INTERVAL = 100; // ms
    const MAX_QUEUE_SIZE = 5000;
    
    // 移除 SERVER_URL，因为不直接 fetch
    // const SERVER_URL = "http://127.0.0.1:8080/api/douyin/packet"; 

    // 双缓冲队列，避免并发问题
    let isSending = false;

    // 1. Hook WebSocket
    window.WebSocket = function (url, protocols) {
        // console.log("🔗 New WebSocket:", url);
        const ws = new OLD_WS(url, protocols);

        // 监听 message 事件
        ws.addEventListener("message", function (event) {
            try {
                // 只处理 ArrayBuffer (protobuf)
                if (event.data instanceof ArrayBuffer) {
                    // 拷贝数据 (非常重要，否则后续可能被释放)
                    const payload = event.data.slice(0);
                    
                    // 放入队列
                    if (MSG_QUEUE.length < MAX_QUEUE_SIZE) {
                        MSG_QUEUE.push({
                            t: Date.now(), // timestamp
                            d: _arrayBufferToBase64(payload) // 转 Base64 方便传输
                        });
                    } else {
                        // console.warn("⚠️ Queue full, dropping packet!");
                    }
                }
                // 打印原始 ArrayBuffer 内容 (调试用)
                console.log("📦打印原始 ArrayBuffer 内容:", event.data);
            } catch (e) {
                console.error("Hook Error:", e);
            }
        });

        return ws;
    };

    // 保持原型链 (骗过部分反爬检测)
    window.WebSocket.prototype = OLD_WS.prototype;
    window.WebSocket.CONNECTING = OLD_WS.CONNECTING;
    window.WebSocket.OPEN = OLD_WS.OPEN;
    window.WebSocket.CLOSING = OLD_WS.CLOSING;
    window.WebSocket.CLOSED = OLD_WS.CLOSED;

    // 2. 定时批量发送
    setInterval(() => {
        if (MSG_QUEUE.length === 0 || isSending) return;

        isSending = true;
        const batch = MSG_QUEUE.splice(0, MSG_QUEUE.length); // 取出所有

        try {
            // 通过 postMessage 发送给 Content Script
            // Content Script 运行在 Isolated World，但能监听 window 的 message 事件
            window.postMessage({
                __DOUYIN_HOOK__: true,
                type: "WS_PACKET_BATCH",
                payload: batch
            }, "*");
            
            console.log(`本次发送给 Content Script ${batch.length} 条数据`);
        } catch (e) {
            console.error("❌ PostMessage failed:", e);
        } finally {
            isSending = false;
        }

    }, FLUSH_INTERVAL);

    // 辅助：ArrayBuffer -> Base64
    function _arrayBufferToBase64(buffer) {
        let binary = '';
        const bytes = new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

})();
