package com.renthouse.enums;

public enum ContractStatus {
    ACTIVE("进行中"),
    EXPIRED("已到期"),
    TERMINATED("已终止"),
    TERMINATION_PENDING("待终止确认"),
    TERMINATION_PENDING_COUNTERPARTY("待对方确认终止"),
    TERMINATION_PENDING_STAFF_REVIEW("待业务员审核终止"),
    TERMINATION_FORCE_PENDING_JOINT_REVIEW("强制终止待联合审核"),
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
