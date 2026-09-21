package com.sja.system.service.async.model;

/**
 * 异步写入任务的生命周期状态。
 */
public enum AsyncTaskStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}
