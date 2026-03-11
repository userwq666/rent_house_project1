package com.renthouse.dto;

import lombok.Data;

/**
 * 消息发送请求
 * receiverId: 接收账号ID（USER/STAFF/ADMIN）
 */
@Data
public class SendMessageRequest {
    private Long receiverId;
    private String title;
    private String content;
}
