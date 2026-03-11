package com.renthouse.security;

import com.renthouse.enums.AccountType;

/**
 * Lightweight JWT principal for unified account model.
 */
public class AuthenticatedUser {
    private final Long accountId;
    private final AccountType accountType;

    public AuthenticatedUser(Long accountId, AccountType accountType) {
        this.accountId = accountId;
        this.accountType = accountType;
    }

    public Long getAccountId() {
        return accountId;
    }

    public AccountType getAccountType() {
        return accountType;
    }
}
