package com.sja.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FeeListVO {

    private Long id;

    private String workType;

    private String title;

    private String deptName;

    private String platform;

    private LocalDate publishDate;

    private BigDecimal totalAmount;

    private String status;

    /**
     * 作品发布状态；月度补贴不适用，值为 null。
     */
    private Integer isPublished;

    private String submitterName;

    private LocalDateTime createTime;

}
