package com.renthouse.enums;

public enum ContractStatus {
    ACTIVE("进行中"),
    EXPIRED("已到期"),
    TERMINATED("已终止"),
    TERMINATION_PENDING("待终止确认"),
    TERMINATION_PENDING_STAFF_REVIEW("待业务员审核终止"),
    PENDING_LANDLORD_APPROVAL("待房东审批"),
    PENDING_STAFF_SIGNING("待业务员签约"),
    PENDING_ADMIN_APPROVAL("待管理员审批");

    private final String description;

    ContractStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
