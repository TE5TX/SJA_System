package com.sja.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class FeeDetailVO {

    private Long id;

    private String workType;

    private Map<String, Object> workInfo;

    private List<ContributorVO> contributors;

    private String status;

    private BigDecimal totalAmount;

    private String remark;

    private LocalDateTime createTime;

    @Data
    public static class ContributorVO {

        private Long userId;

        private String realName;

        private String roleInWork;

        private BigDecimal feeAmount;

    }

}
