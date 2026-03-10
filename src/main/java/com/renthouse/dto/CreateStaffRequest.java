package com.renthouse.dto;

import lombok.Data;

@Data
public class CreateStaffRequest {
    private String username;
    private String password;
    private String displayName;
    private String phone;
}
