package com.renthouse.security;

import com.renthouse.enums.OperatorRole;

/**
 * Lightweight auth principal for both end users and operators.
 */
public class AuthenticatedUser {
    private final Long userId;
    private final Long accountId;
    private final Long operatorId;
    private final OperatorRole operatorRole;

    private AuthenticatedUser(Long userId, Long accountId, Long operatorId, OperatorRole operatorRole) {
        this.userId = userId;
        this.accountId = accountId;
        this.operatorId = operatorId;
        this.operatorRole = operatorRole;
    }

    public static AuthenticatedUser forUser(Long userId, Long accountId) {
        return new AuthenticatedUser(userId, accountId, null, null);
    }

    public static AuthenticatedUser forOperator(Long operatorId, OperatorRole operatorRole) {
        return new AuthenticatedUser(null, null, operatorId, operatorRole);
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public OperatorRole getOperatorRole() {
        return operatorRole;
    }

    public boolean isOperator() {
        return operatorId != null;
    }
}
