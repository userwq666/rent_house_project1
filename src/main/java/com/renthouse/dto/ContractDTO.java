package com.renthouse.dto;

import com.renthouse.enums.ContractStatus;
import com.renthouse.enums.TerminationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同信息DTO
 */
@Data
public class ContractDTO {
    private Long id;
    private Long houseId;
    private String houseTitle;
    private String houseAddress;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private Long tenantId;
    private String tenantName;
    private String tenantPhone;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private LocalDate startDate;
    private LocalDate endDate;
    private ContractStatus status;
    private LocalDateTime signedDate;
    private String notes;
    private LocalDateTime createdAt;
    private TerminationStatus terminationStatus;
    private Long terminationRequestId;
}
