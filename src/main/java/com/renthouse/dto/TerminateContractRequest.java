package com.renthouse.dto;

import lombok.Data;

/**
 * 发起合同终止请求 DTO
 */
@Data
public class TerminateContractRequest {
    private String reason;
    private Boolean force = false;
    private String forceReason;
}
