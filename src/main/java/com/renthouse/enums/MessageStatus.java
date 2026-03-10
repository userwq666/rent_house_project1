package com.renthouse.enums;

/**
 * 消息状态
 */
public enum MessageStatus {
    UNREAD,
    READ,
    PENDING,   // 待处理（需要操作）
    ACCEPT,    // 已同意
    REJECT,    // 已拒绝
    ARCHIVED   // 已归档/过时（前端隐藏，数据库保留）
}
