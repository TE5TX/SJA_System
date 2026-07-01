package com.sja.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {

    private Long id;

    private Long userId;

    private String realName;

    private String roleName;

    private String operationType;

    private String targetModule;

    private Long targetId;

    private String detail;

    private String ipAddress;

    private LocalDateTime createTime;

}
