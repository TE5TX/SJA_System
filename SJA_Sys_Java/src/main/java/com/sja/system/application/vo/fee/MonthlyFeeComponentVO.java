package com.sja.system.application.vo.fee;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyFeeComponentVO {

    private Long userId;
    private BigDecimal feeAmount;
    private String subsidyCategory;
}
