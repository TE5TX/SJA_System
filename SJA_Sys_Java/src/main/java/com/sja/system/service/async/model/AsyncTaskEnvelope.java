package com.sja.system.service.async.model;

import lombok.Builder;
import lombok.Data;

/**
 * Redis Stream 中保存的任务信封。
 * payload 只保存 AES-GCM 密文，避免个人敏感字段以明文进入 Redis。
 */
@Data
@Builder
public class AsyncTaskEnvelope {

    private String taskId;
    private AsyncTaskType type;
    private Long ownerId;
    private String payload;
    private String idempotencyKey;
    private Integer attempt;
    private String createdAt;
}
