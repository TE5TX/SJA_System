package com.sja.system.service.async.model;

/**
 * 当前应用实例在 Redis Stream 消费者组中的唯一身份。
 *
 * @param name Redis Stream 消费者名称
 * @param instanceId 当前进程的租约所有者标识
 * @param source 消费者名称来源
 */
// 202609201556 - 完成了Redis Stream消费者实例身份的统一建模任务。
public record AsyncTaskConsumerIdentity(String name, String instanceId, String source) {
}
