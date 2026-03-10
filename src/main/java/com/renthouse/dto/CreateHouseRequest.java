package com.renthouse.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建房源请求DTO
 */
@Data
public class CreateHouseRequest {
    private String title;
    private String address;
    private String district;
    private String houseType;
    private BigDecimal area;
    private Integer floor;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private String description;
    private String images;
    private String facilities;
}
