package com.renthouse.dto;

import lombok.Data;

/**
 * 用户之间发送消息请求
 */
@Data
public class SendMessageRequest {
    private Long receiverId;
    private String title;
    private String content;
}
