package com.sja.system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncTaskPayloadCipherTest {

    @Test
    void encryptsPayloadAndBindsCiphertextToTaskContext() {
        AsyncTaskPayloadCipher cipher = new AsyncTaskPayloadCipher(
                "queue-encryption-key-with-more-than-32-characters", "unused-jwt-secret");
        String plaintext = "{\"idCard\":\"440000000000000000\"}";

        String encrypted = cipher.encrypt(plaintext, "task-1|PROFILE_UPDATE");

        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, cipher.decrypt(encrypted, "task-1|PROFILE_UPDATE"));
        assertThrows(IllegalStateException.class,
                () -> cipher.decrypt(encrypted, "task-2|PROFILE_UPDATE"));

        String statusToken = cipher.deriveStatusToken("task-1");
        assertEquals(statusToken, cipher.deriveStatusToken("task-1"));
        assertNotEquals(statusToken, cipher.deriveStatusToken("task-2"));
    }
}
