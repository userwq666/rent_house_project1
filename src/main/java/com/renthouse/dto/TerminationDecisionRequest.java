package com.renthouse.dto;

import lombok.Data;

/**
 * 终止申请审批请求
 */
@Data
public class TerminationDecisionRequest {
    private Boolean approve;
    private String comment;
}
