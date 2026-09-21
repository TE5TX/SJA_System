package com.sja.system.service.async.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncTaskConsumerIdentityFactoryTest {

    @Test
    void keepsNormalizedExplicitConsumerName() {
        AsyncTaskConsumerIdentity identity = AsyncTaskConsumerIdentityFactory.create(
                " Backend Node 01 ", "ignored", 10L, "12345678", "instance-1");

        assertEquals("backend-node-01", identity.name());
        assertEquals("configuration", identity.source());
    }

    @Test
    void createsDifferentNamesForInstancesOnSameHostAndProcess() {
        AsyncTaskConsumerIdentity first = AsyncTaskConsumerIdentityFactory.create(
                "", "backend-1", 42L, "aaaaaaaa", "instance-1");
        AsyncTaskConsumerIdentity second = AsyncTaskConsumerIdentityFactory.create(
                null, "backend-1", 42L, "bbbbbbbb", "instance-2");

        // 202609201556 - 验证同一主机与进程信息下仍可生成不同消费者名称。
        assertNotEquals(first.name(), second.name());
        assertTrue(first.name().startsWith("sja-write-worker-backend-1-42-"));
        assertEquals("runtime", first.source());
    }

    @Test
    void rejectsExplicitNameThatContainsNoUsableCharacters() {
        assertThrows(IllegalArgumentException.class, () -> AsyncTaskConsumerIdentityFactory.create(
                "中文消费者", "backend", 42L, "aaaaaaaa", "instance-1"));
    }

    @Test
    void limitsAutomaticallyGeneratedNameLength() {
        String longHost = "backend".repeat(40);

        AsyncTaskConsumerIdentity identity = AsyncTaskConsumerIdentityFactory.create(
                null, longHost, 42L, "aaaaaaaa", "instance-1");

        assertTrue(identity.name().length() <= 128);
    }
}
