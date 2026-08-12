package com.sja.system.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyFeeComponentVO {

    private Long userId;
    private BigDecimal feeAmount;
    private String subsidyCategory;
}
