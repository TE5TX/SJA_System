package com.sja.system.entity;

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
@TableName("biz_work_contributor")
public class BizWorkContributor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workType;

    private Long workId;

    private Long userId;

    private String roleInWork;

    private BigDecimal feeAmount;

    private String remark;

    private LocalDateTime createTime;

}
