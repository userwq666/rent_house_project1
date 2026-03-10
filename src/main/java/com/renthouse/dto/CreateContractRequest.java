package com.renthouse.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建合同请求DTO
 */
@Data
public class CreateContractRequest {
    private Long houseId;
    private Long tenantId;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}
