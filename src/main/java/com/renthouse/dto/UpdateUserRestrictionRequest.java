package com.renthouse.dto;

import lombok.Data;

/**
 * 更新用户权限请求
 */
@Data
public class UpdateUserRestrictionRequest {
    private Boolean canPublish;
    private Boolean canRent;
}
