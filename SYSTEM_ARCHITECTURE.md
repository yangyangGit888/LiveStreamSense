# 抖音直播数据采集系统 - 架构设计文档

## 1. 系统简介与核心价值

### 1.1 项目定位
本项目是一个基于 **浏览器自动化 (Playwright)** 与 **Chrome Extension (Manifest V3)** 技术的高性能抖音直播间数据采集系统。它专为解决高实时性、高并发的直播弹幕抓取需求而设计。

### 1.2 解决的核心痛点
在爬虫领域，抓取抖音直播数据面临两大难题：
1.  **协议极其复杂**：抖音网页版直播使用 WebSocket 传输，数据采用 Protobuf 二进制序列化，且包含动态变化的签名加密算法（Signature）。逆向破解这些协议不仅难度极高，而且一旦抖音更新算法，维护成本巨大。
2.  **DOM 解析效率低**：传统的 Selenium 爬虫通过轮询页面 HTML 元素来获取弹幕，效率极低，CPU 占用高，且在高频弹幕场景下极易丢失数据。

### 1.3 核心解决方案
本系统采用了 **"中间人劫持" (Man-in-the-Browser)** 的创新方案：
*   **不破解加密协议**：我们不尝试去解密 WebSocket 流量。
*   **直接截取内存对象**：我们利用 JavaScript Hook 技术，在浏览器内存中，拦截**已经被抖音前端代码解密完成的**业务对象。
*   **相当于**：我们不当“破译专家”，而是直接在“接收者”手中把信件复印了一份。

---

## 2. 技术架构与选型

系统采用 **B/S 混合架构**，分为 **采集端 (Browser)** 和 **处理端 (Server)**。

```mermaid
graph LR
    subgraph Browser [采集端: Chrome/Edge]
        Hook[page.js (Hook)] -->|拦截| DouyinApp[抖音前端应用]
        Hook -->|转发| Content[content.js (Bridge)]
        Content -->|缓冲| Background[background.js (Sender)]
    end
    
    Background -->|HTTP POST| JavaServer[处理端: Java Spring Boot]
    
    subgraph Server [后端服务]
        JavaServer -->|分发| Dispatcher
        Dispatcher -->|解析| ProtoHandlers
        ProtoHandlers -->|业务| Logging/DB
    end
```

### 技术栈清单
| 模块 | 技术选型 | 核心作用 |
| :--- | :--- | :--- |
| **浏览器控制** | **Microsoft Playwright** | 负责浏览器的生命周期管理、自动启动、页面导航、脚本注入。比 Selenium 更快、更稳定。 |
| **前端拦截** | **JavaScript Hook** | 劫持 `Array.prototype.push`，这是抖音前端分发消息的必经之路。 |
| **插件架构** | **Chrome Extension V3** | 利用 Manifest V3 的 Service Worker 特性，实现独立于页面的稳定后台通信。 |
| **后端框架** | **Spring Boot 2.7** | 提供高性能 HTTP 接收接口，管理业务逻辑。 |
| **数据序列化** | **Google Protobuf** | 用于在 Java 端反序列化采集到的二进制数据，提取精确字段。 |

---

## 3. 核心模块详解

### 3.1 浏览器采集层 (Chrome Extension)
这部分是系统的"眼睛"和"手"，运行在浏览器内部。

*   **`page.js` (潜伏者)**
    *   **位置**：注入到网页的主上下文 (Main World)。
    *   **职责**：Hook `Array.prototype.push`。当抖音收到弹幕并解码为对象后，会调用 push 放入消息队列。此时 `page.js` 瞬间捕获该对象，复制一份，并通过 `postMessage` 传出。
    *   **关键点**：只拦截白名单消息（如弹幕、礼物），极大减少无效数据。

*   **`content.js` (翻译官)**
    *   **位置**：运行在隔离环境 (Isolated World)。
    *   **职责**：作为 page.js 和 background.js 的桥梁。接收消息后，将二进制数据 (`Uint8Array`) 转码为 **Base64** 字符串（防止 JSON 传输乱码），并推入内存缓冲区。

*   **`background.js` (快递员)**
    *   **位置**：Chrome Service Worker (后台进程)。
    *   **职责**：维护一个发送队列。通过定时器，将缓冲区的数据批量、串行地 POST 给 Java 后端。
    *   **可靠性设计**：如果 Java 后端接口报错或超时，它会将这批数据**重新放回队列头部**，确保网络抖动时数据不丢失。

### 3.2 服务端处理层 (Java)
这部分是系统的"大脑"，运行在本地服务器。

*   **`DouyinCrawlerService`**
    *   负责启动 Playwright 浏览器实例。
    *   在浏览器启动时，自动加载我们编写的 Extension。
    *   在页面加载前，强制注入 Hook 脚本，确保不错过任何初始化消息。

*   **`DouyinDataController`**
    *   提供 `/api/douyin/im` 接口，接收来自 Extension 的批量数据。

*   **`MessageDispatcher` & `Handlers`**
    *   **分发器**：根据消息的 `method` 字段（如 `WebcastChatMessage`），将数据路由给对应的处理器。
    *   **处理器**：使用 Protobuf 生成的 Java 类，将 Base64 解码后的字节流反序列化为 Java 对象，提取用户昵称、弹幕内容、礼物价值等核心信息。

---

## 4. 全链路工作流程

1.  **系统启动**：用户运行 Spring Boot 应用，调用 `/launch` 接口。
2.  **浏览器就绪**：Playwright 启动 Edge 浏览器，加载 Extension，打开抖音主页。
3.  **进入直播间**：用户扫码登录或直接输入直播间 ID 进入。
4.  **数据流转**：
    *   **抖音端**：WebSocket 收到加密数据 -> JS 解密 -> 生成 Message 对象 -> `push` 到数组。
    *   **拦截端**：`page.js` 拦截 `push` -> 复制对象 -> 发送给 `content.js`。
    *   **中转端**：`content.js` 转 Base64 -> 存入 Buffer -> 批量发给 `background.js`。
    *   **传输端**：`background.js` 队列管理 -> HTTP POST -> Java 后端。
5.  **数据落地**：Java 后端接收 -> Protobuf 解析 -> 打印日志或存入数据库。

---

## 5. 核心优势与设计亮点

1.  **极低的维护成本 (Zero Reverse Engineering)**
    *   避开了最困难的签名算法和协议逆向。只要抖音前端还用 JavaScript 解码数据，这套方案就有效。
2.  **企业级的高可靠性**
    *   **MV3 Service Worker**：使用 Chrome 最新标准，后台进程独立存活，页面卡顿不影响数据发送。
    *   **断点续传**：前端维护重试队列，后端服务重启或网络闪断期间，数据会暂存在浏览器内存中，恢复后自动补发。
3.  **高性能**
    *   浏览器端只做搬运，不解析 Protobuf，CPU 占用极低。
    *   Java 端利用多线程和 Netty (Spring Web) 高效处理业务逻辑。
4.  **数据完整性**
    *   直接获取内存对象，包含了 HTML 页面上没有的隐藏字段（如用户真实 ID、礼物连击统计、详细榜单数据）。

---

## 6. 未来优化方向

1.  **数据持久化增强**
    *   在 Extension 端引入 **IndexedDB**，即使浏览器崩溃，未发送的数据也能在下次启动时恢复。
    *   后端接入 MySQL 或 ClickHouse，进行数据存储和 BI 分析。
2.  **多直播间并发**
    *   目前支持单浏览器多 Tab 采集。未来可在 Java 端建立 `RoomContext`，同时管理多个 Playwright 实例或 Page，实现大规模矩阵采集。
3.  **动态 Protobuf 加载**
    *   目前 Protobuf 文件是编译时生成的。可以优化为运行时动态加载 `.desc` 描述文件，无需重启 Java 服务即可支持解析新的消息类型。
4.  **反爬对抗**
    *   如果抖音检测 `Array.prototype.push` Hook，可以升级为 CDP (Chrome DevTools Protocol) 监听方案，或者使用更底层的 `Object.defineProperty` 进行劫持。
