package com.mall.notice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一给前端的 WS 消息格式：{ type, data }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage {
    private String type;
    private Object data;
}
