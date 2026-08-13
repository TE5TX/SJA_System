package com.sja.system.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUserVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializedSessionIdentityDoesNotExposeSensitiveProfileFields() throws Exception {
        SessionUserVO user = new SessionUserVO();
        user.setId(1L);
        user.setUsername("20240001");
        user.setRealName("Test User");

        String json = objectMapper.writeValueAsString(user);

        assertTrue(json.contains("username"));
        assertTrue(json.contains("realName"));
        assertFalse(json.contains("phone"));
        assertFalse(json.contains("idCard"));
        assertFalse(json.contains("bankCard"));
        assertFalse(json.contains("dormAddress"));
    }
}
