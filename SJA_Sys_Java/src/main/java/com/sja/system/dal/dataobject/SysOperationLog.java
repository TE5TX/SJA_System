package com.sja.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String realName;

    private String roleName;

    private String operationType;

    private String targetModule;

    private Long targetId;

    private String detail;

    private String ipAddress;

    private String requestId;

    private String requestMethod;

    private String requestUri;

    private String resultStatus;

    private String beforeData;

    private String afterData;

    private String userAgent;

    private Long durationMs;

    private String remark;

    private LocalDateTime createTime;

}
