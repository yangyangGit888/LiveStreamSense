// content.js - Chrome Extension Content Script (Isolated World)
// 职责：纯缓冲层，接收 page.js 的消息，批量转发给 background
// 约束：无业务逻辑，无网络请求

console.log("[DouyinHook-Ext] Content Script Loaded (IM Buffer Mode)");

const BUFFER = [];
const FLUSH_INTERVAL = 200; // ms
const MAX_BUFFER_SIZE = 3000;

// 监听 Page Context 消息
window.addEventListener("message", function(event) {
    if (event.source !== window) return;

    const data = event.data;
    // 校验标记
    if (data && data.__DOUYIN_IM__ === true) {
        
        // 将 Uint8Array 转为 Array (JSON 序列化需要) 或者保持原样？
        // Chrome runtime.sendMessage 支持传递 Uint8Array 吗？支持的。
        // 但为了后续 JSON.stringify 方便，这里转为普通数组 (Array.from) 比较稳妥，
        // 或者在 background 再转。
        // 为了传输效率，先保持 Uint8Array。
        // 但是 JSON.stringify(Uint8Array) 会变成 {"0":1, "1":2...} 这种对象，体积爆炸。
        // 所以最好在这里或者 background 转 Base64。
        // 用户 prompt 要求：
        // content.js 禁止 protobuf / gzip
        // background fetch Content-Type: application/json
        // 所以最终发给 Java 的 JSON 里，payload 最好是 Base64 字符串。
        // 为了 content.js 逻辑简单（"傻逻辑"），我们可以在这里转 Base64，或者交给 background。
        // 考虑到 content.js "禁止业务逻辑"，但 Base64 属于传输编码。
        // 让我们在 push 到 buffer 前转 Base64 吧，这样 buffer 里存的就是准备好的数据。
        // 
        // 等等，用户 prompt 说 content.js "push 到内存 buffer"，然后 "sendMessage"。
        // 没说要转 Base64。但 sendMessage 到 background 是内部通信。
        // background fetch 到 Java 需要 JSON。
        // 那我们在 content.js 做 Base64 转换比较合适，分摊计算压力。
        
        if (BUFFER.length < MAX_BUFFER_SIZE) {
            // 构造 buffer item
            const item = {
                method: data.method,
                payload: _uint8ArrayToBase64(data.payload), // 转 Base64
                ts: data.ts
            };
            BUFFER.push(item);
        } else {
            // 缓冲满，丢弃最旧数据 (shift)
            BUFFER.shift();
            // 再 push 新的
            const item = {
                method: data.method,
                payload: _uint8ArrayToBase64(data.payload),
                ts: data.ts
            };
            BUFFER.push(item);
        }
    }
});

// 定时批量发送给 Background
setInterval(() => {
    if (BUFFER.length === 0) return;

    // 取出所有数据
    const batch = BUFFER.splice(0, BUFFER.length);

    try {
        chrome.runtime.sendMessage({
            type: "IM_BATCH",
            payload: batch
        });
    } catch (e) {
        // Extension 上下文可能失效
    }

}, FLUSH_INTERVAL);

// 辅助：Uint8Array -> Base64
function _uint8ArrayToBase64(bytes) {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}
