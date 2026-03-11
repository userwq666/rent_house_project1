package com.renthouse.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperatorAccountResponse {
    private Long id;
    private String username;
    private String displayName;
    private String phone;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
