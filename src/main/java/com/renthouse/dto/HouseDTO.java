package com.renthouse.dto;

import com.renthouse.enums.HouseStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋信息DTO
 */
@Data
public class HouseDTO {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String address;
    private String district;
    private String houseType;
    private BigDecimal area;
    private Integer floor;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private HouseStatus status;
    private String description;
    private String images;
    private String facilities;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long currentTenantId;
    private String currentTenantName;
    private String currentTenantPhone;
    private Long assignedStaffId;
    private String reviewComment;
    private LocalDateTime reviewedAt;
}
