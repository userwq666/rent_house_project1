package com.renthouse.dto;

import com.renthouse.enums.OperatorRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperatorAccountResponse {
    private Long id;
    private String username;
    private String displayName;
    private String phone;
    private OperatorRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
