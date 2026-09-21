package com.sja.system.framework.config;

import com.sja.system.service.async.model.AsyncTaskConsumerIdentity;
import com.sja.system.service.async.model.AsyncTaskConsumerIdentityFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Redis Stream 消费者运行时身份配置。
 */
@Slf4j
@Configuration
public class AsyncTaskConsumerIdentityConfiguration {

    @Bean
    public AsyncTaskConsumerIdentity asyncTaskConsumerIdentity(AsyncTaskProperties properties,
                                                               Environment environment) {
        String host = firstText(environment.getProperty("HOSTNAME"),
                environment.getProperty("COMPUTERNAME"), localHostName());
        String instanceId = UUID.randomUUID().toString();
        String randomSuffix = instanceId.substring(0, 8);
        AsyncTaskConsumerIdentity identity = AsyncTaskConsumerIdentityFactory.create(
                properties.getConsumerName(), host, ProcessHandle.current().pid(), randomSuffix, instanceId);
        // 202609201556 - 启动时只生成一次消费者身份，监听与pending恢复共用同一名称。
        log.info("Resolved Redis Stream consumer identity: consumer={}, source={}",
                identity.name(), identity.source());
        return identity;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "unknown-host";
    }

    private String localHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return null;
        }
    }
}
