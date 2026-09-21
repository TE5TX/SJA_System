package com.sja.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 异步写入队列参数。默认值针对 2 核、2GB 的单机部署设置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sja.async-task")
public class AsyncTaskProperties {

    private boolean enabled = true;
    private String streamKey = "sja:async:write-tasks";
    private String consumerGroup = "sja-write-workers";
    private String consumerName;
    private int workerCount = 4;
    private int workerQueueCapacity = 500;
    private long queueCapacity = 5000;
    private int maxRetries = 2;
    private Duration pollTimeout = Duration.ofSeconds(2);
    private Duration visibilityTimeout = Duration.ofMinutes(5);
    private Duration statusTtl = Duration.ofHours(24);
    private Duration processingLockTtl = Duration.ofMinutes(6);
}
