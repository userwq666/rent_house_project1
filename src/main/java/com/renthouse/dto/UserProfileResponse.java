package com.renthouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private Long accountId;
    private String username;
    private String accountType;
    private String realName;
    private String phone;
    private String email;
    private String idCard;
}
