package com.mall.notice.model;

import lombok.Data;

@Data
public class NotifyBroadcastPayload {
    private Long userId;      // 目标用户
    private String type;      // 事件类型，例如 ORDER_STATUS_CHANGED
    private String bodyJson;  // 实际要推给前端的数据（或直接用 Map）
}
