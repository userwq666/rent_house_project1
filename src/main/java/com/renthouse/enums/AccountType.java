package com.renthouse.enums;

/**
 * 账号类型枚举
 */
public enum AccountType {
    ADMIN("管理员"),
    USER("普通用户");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
