package com.renthouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private Long operatorId;
    private String username;
    private String userType;
    private String principalType;
    private String message;

    public AuthResponse(String token, Long userId, String username, String userType, String message) {
        this(token, userId, null, username, userType, "USER", message);
    }

    public static AuthResponse operator(String token, Long operatorId, String username, String userType, String message) {
        return new AuthResponse(token, null, operatorId, username, userType, "OPERATOR", message);
    }
}
