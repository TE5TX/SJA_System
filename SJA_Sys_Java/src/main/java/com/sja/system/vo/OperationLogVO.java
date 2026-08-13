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

    private String requestId;

    private String requestMethod;

    private String requestUri;

    private String resultStatus;

    private String beforeData;

    private String afterData;

    private String userAgent;

    private Long durationMs;

    private LocalDateTime createTime;

}
