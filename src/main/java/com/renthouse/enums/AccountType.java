package com.renthouse.enums;

public enum AccountType {
    ADMIN("管理员"),
    STAFF("业务员"),
    USER("普通用户");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
