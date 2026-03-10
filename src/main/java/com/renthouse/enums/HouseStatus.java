package com.renthouse.enums;

/**
 * 房源状态枚举
 */
public enum HouseStatus {
    PENDING_STAFF_REVIEW("待业务员审核"),
    AVAILABLE("可租"),
    PENDING("等待中"),
    RENTED("已出租"),
    OFFLINE("已下架");

    private final String description;

    HouseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
