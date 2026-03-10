package com.renthouse.security;

/**
 * 简单的认证主体，封装当前登录用户和账号信息。
 */
public class AuthenticatedUser {
    private final Long userId;
    private final Long accountId;

    public AuthenticatedUser(Long userId, Long accountId) {
        this.userId = userId;
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
