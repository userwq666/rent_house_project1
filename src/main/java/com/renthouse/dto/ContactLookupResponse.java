package com.renthouse.dto;

import lombok.Data;

@Data
public class ContactLookupResponse {
    private String principalType; // USER / OPERATOR
    private Long userId;
    private Long operatorId;
    private String username;
    private String displayName;
    private String phone;
}

