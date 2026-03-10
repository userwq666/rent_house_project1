package com.renthouse.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户信息响应
 */
@Data
public class UserAdminResponse {
    private Long userId;
    private Long accountId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String accountType;
    private Boolean enabled;
    private Boolean canPublish;
    private Boolean canRent;
    private LocalDateTime createdAt;
}
