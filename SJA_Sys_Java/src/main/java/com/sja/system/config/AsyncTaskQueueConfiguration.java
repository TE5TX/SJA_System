package com.sja.system.config;

import com.sja.system.service.AsyncTaskQueueService;
import com.sja.system.service.AsyncTaskWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Redis Stream 监听器与有限工作线程池配置。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "sja.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTaskQueueConfiguration {

    @Bean("asyncTaskWorkerExecutor")
    public ThreadPoolTaskExecutor asyncTaskWorkerExecutor(AsyncTaskProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getWorkerCount());
        executor.setMaxPoolSize(properties.getWorkerCount());
        executor.setQueueCapacity(properties.getWorkerQueueCapacity());
        executor.setThreadNamePrefix("sja-write-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 202608192148 - 修复了队列瞬时堆积时继续无限拉取导致内存上涨的问题。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService asyncTaskStreamPollExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sja-write-stream-poller");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> asyncTaskStreamContainer(
            RedisConnectionFactory connectionFactory,
            AsyncTaskProperties properties,
            AsyncTaskQueueService queueService,
            AsyncTaskWorker worker,
            @Qualifier("asyncTaskStreamPollExecutor") ExecutorService pollExecutor) {
        queueService.initializeConsumerGroup();

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String,
                MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(properties.getPollTimeout())
                        .batchSize(Math.max(1, properties.getWorkerCount()))
                        .executor(pollExecutor)
                        .errorHandler(error -> log.error("Redis Stream listener failed", error))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(
                Consumer.from(properties.getConsumerGroup(), properties.getConsumerName()),
                StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed()),
                worker::accept);
        return container;
    }
}
