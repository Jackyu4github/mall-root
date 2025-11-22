package com.mall.notice.model;

import lombok.Data;

@Data
public class WsBroadcastPayload {
    private String targetType;   // "SYS_USER"/"APP_USER"（可选）
    private Long userId;         // null=广播；非 null=点对点
    private String eventType;    // 事件名
    private String bodyJson;     // 业务体 json 字符串
    private Long timestamp;      // 可选：发出时间
    private String eventId;      // 可选：去重用
}
