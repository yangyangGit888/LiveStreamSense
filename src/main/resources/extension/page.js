// page.js - 运行在页面上下文 (Injected Script)
// 职责：Hook Array.prototype.push，拦截抖音已解码的 IM 消息对象
// 约束：只转发白名单内的消息，不进行任何解码或复杂操作

(function() {
    'use strict';

    // 消息类型白名单
    const TARGET_METHODS = new Set([
        "WebcastChatMessage",
        "WebcastLikeMessage",
        "WebcastGiftMessage",
        "WebcastMemberMessage",
        "WebcastRoomStatsMessage",
        "WebcastRanklistHourEntranceMessage"
    ]);

    const originalPush = Array.prototype.push;

    // Hook Array.prototype.push
    Array.prototype.push = function(...items) {
        try {
            for (const item of items) {
                // 快速特征匹配：必须是对象，且包含 method 属性
                if (item && typeof item === 'object' && item.method) {
                    // 1. 检查是否在白名单中
                    if (TARGET_METHODS.has(item.method)) {
                        // 2. 检查 payload 是否为 Uint8Array (二进制数据)
                        // 注意：拦截的是已解码层的对象，通常 payload 是 raw bytes
                        // 如果抖音前端已经解开 payload 变成了具体字段，这里可能就没有 payload 字段了
                        // 根据用户提示 "拦截抖音 JS 已解码后的 IM 消息对象"，
                        // 这里的 item 很可能已经是 Protobuf 解码后的 Object。
                        // 但如果是 "拦截 JS 已解码后的 IM 消息对象"，那么 payload 就不一定是 Uint8Array 了，
                        // 而是具体的字段 (如 content, user 等)。
                        // 
                        // 然而，用户的 prompt 中写道：
                        // "命中条件：item.payload 是 Uint8Array"
                        // 这意味着我们 Hook 的时机是在 "外层 Response 解包后，内层 Payload 解包前" 的那个数组 push。
                        // 抖音 Web 端通常会把解出的 Message 对象（含 method 和 payload bytes）push 到一个数组里。
                        
                        if (item.payload && (item.payload instanceof Uint8Array)) {
                            // 3. 构造转发数据
                            // 必须复制 payload，防止后续被修改或释放
                            const safePayload = item.payload.slice(0);
                            
                            window.postMessage({
                                __DOUYIN_IM__: true,
                                method: item.method,
                                payload: safePayload, // 这里 postMessage 会自动序列化 Uint8Array
                                ts: Date.now()
                            }, "*");
                        }
                    }
                }
            }
        } catch (e) {
            // 禁止 console.log，静默失败
        }
        
        // 执行原始逻辑
        return originalPush.apply(this, items);
    };

    console.log("[DouyinHook] Array.prototype.push Hooked (IM Mode)");

})();
