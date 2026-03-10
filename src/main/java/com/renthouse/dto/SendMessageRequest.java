package com.renthouse.dto;

import lombok.Data;

/**
 * 消息发送请求
 * receiverId: 接收普通用户ID
 * receiverOperatorId: 接收操作员ID（管理员/业务员）
 */
@Data
public class SendMessageRequest {
    private Long receiverId;
    private Long receiverOperatorId;
    private String title;
    private String content;
}
