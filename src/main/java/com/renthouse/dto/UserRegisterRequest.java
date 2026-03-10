package com.renthouse.dto;

import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class UserRegisterRequest {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String idCard;
}
