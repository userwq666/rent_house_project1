package com.renthouse.dto;

import lombok.Data;

/**
 * 更新账号状态请求
 */
@Data
public class UpdateAccountStatusRequest {
    private Boolean enabled;
}
