# 抖音直播协议字段字典 (DOUYIN_PROTO_FIELDS.md)

本文档统计了系统中使用的主要 Protobuf 消息结构及其字段含义。基于 `douyin_hack.proto` 定义。

## 1. 公共结构 (Common Structures)

### Common (通用头部)
所有消息都会包含的基础元数据。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `method` | string | 消息方法名 (如 WebcastChatMessage) |
| `msg_id` | int64 | 消息唯一 ID |
| `room_id` | int64 | 直播间 ID |
| `create_time` | int64 | 消息创建时间戳 |

### User (用户信息)
表示直播间内的用户（观众/主播）。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | int64 | 用户唯一 ID |
| `short_id` | int64 | 用户短 ID (展示用) |
| `nick_name` | string | 用户昵称 |
| `gender` | int32 | 性别 (0:未知, 1:男, 2:女) |
| `signature` | string | 个性签名 |
| `level` | int32 | 等级 |
| `birthday` | int64 | 生日 |
| `telephone` | string | 手机号 (通常加密或脱敏) |
| `avatar_thumb` | Image | 头像缩略图 |
| `avatar_medium` | Image | 头像中图 |
| `avatar_large` | Image | 头像大图 |

### Image (图片结构)
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `url_list` | string[] | 图片 URL 列表 (通常有多个 CDN 地址) |
| `uri` | string | 图片唯一标识 URI |

---

## 2. 核心消息定义

### WebcastChatMessage (弹幕消息)
用户发送的聊天弹幕。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `common` | Common | 通用头部 |
| `user` | User | 发送弹幕的用户信息 |
| `content` | string | 弹幕文本内容 |
| `visible_to_sender` | bool | 是否对自己可见 |
| `background_image` | Image | 弹幕背景图 (气泡) |
| `full_screen_text_color` | string | 全屏模式文字颜色 |
| `background_image_v2` | Image | 新版背景图 |

### WebcastLikeMessage (点赞消息)
用户点击屏幕产生的点赞（爱心）。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `common` | Common | 通用头部 |
| `count` | int64 | 本次点击产生的点赞数 (通常为 1 或连击数) |
| `total` | int64 | 房间当前总点赞数 |
| `color` | int64 | 爱心颜色 ID |
| `user` | User | 点赞的用户信息 |
| `icon` | string | 爱心图标 URL |

### WebcastGiftMessage (礼物消息)
用户送出的礼物。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `common` | Common | 通用头部 |
| `gift_id` | int64 | 礼物 ID |
| `fan_ticket_count` | int64 | 礼物价值 (音浪/抖币) |
| `group_count` | int64 | 分组数量 (批量送礼时使用) |
| `repeat_count` | int64 | 重复数量 (连击数) |
| `combo_count` | int64 | 连击总数 |
| `user` | User | 送礼用户 |
| `to_user` | User | 接收礼物的用户 (主播) |
| `repeat_end` | int32 | 是否结束连击 |

### WebcastMemberMessage (进房消息)
用户进入直播间。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `common` | Common | 通用头部 |
| `user` | User | 进入房间的用户 |
| `member_count` | int64 | 当前房间实时在线人数 |
| `operator` | User | 操作者 (如有) |
| `is_set_to_admin` | bool | 是否被设为管理员 |
| `is_top_user` | bool | 是否是榜单用户 |
| `rank_score` | int64 | 榜单分数 |
| `top_user_no` | int64 | 榜单排名 |
| `enter_type` | int64 | 进入类型 |

### WebcastRoomStatsMessage (房间统计)
房间维度的统计信息（如在线人数）。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `common` | Common | 通用头部 |
| `display_short` | string | 短展示文本 (如 "1.2万") |
| `display_middle` | string | 中展示文本 |
| `display_long` | string | 长展示文本 |
| `display_value` | int64 | 实际数值 (如 12345) |
| `display_version` | int64 | 版本号 |

### WebcastRanklistHourEntranceMessage (小时榜)
小时榜单入口信息。
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `common` | Common | 通用头部 |
| `type` | int32 | 榜单类型 |
| `title` | string | 标题 |
| `sub_title` | string | 副标题 |
| `expire_time` | int64 | 过期时间 |
