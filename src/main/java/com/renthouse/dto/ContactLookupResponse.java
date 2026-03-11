package com.renthouse.dto;

import lombok.Data;

@Data
public class ContactLookupResponse {
    private Long accountId;
    private String accountType;
    private String username;
    private String displayName;
    private String phone;
}
