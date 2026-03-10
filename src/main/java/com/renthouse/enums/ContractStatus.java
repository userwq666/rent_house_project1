package com.renthouse.enums;

/**
 * 合同状态枚举
 */
public enum ContractStatus {
    ACTIVE("进行中"),
    EXPIRED("已到期"),
    TERMINATED("已终止"),
    TERMINATION_PENDING("待终止确认"),
    PENDING_LANDLORD_APPROVAL("待房主审批"),
    PENDING_ADMIN_APPROVAL("待管理员审批");

    private final String description;

    ContractStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
