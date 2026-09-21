package com.sja.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("biz_fee_standard")
public class BizFeeStandard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptCode;

    private String platform;

    private String category;

    private String ruleKey;

    private BigDecimal ruleValue;

    private String description;

    private Integer isActive;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
