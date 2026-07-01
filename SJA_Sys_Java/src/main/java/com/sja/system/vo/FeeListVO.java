package com.sja.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeListVO {

    private Long id;

    private String workType;

    private String title;

    private String deptName;

    private BigDecimal totalAmount;

    private String status;

    private String submitterName;

    private LocalDateTime createTime;

}
