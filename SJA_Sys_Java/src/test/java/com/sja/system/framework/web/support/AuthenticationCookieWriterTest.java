package com.sja.system.framework.web.support;

import com.sja.system.application.model.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationCookieWriterTest {

    @Test
    void writesHttpOnlyStrictAuthenticationCookies() {
        AuthenticationCookieWriter writer = new AuthenticationCookieWriter(900_000L, 604_800_000L, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, new TokenPair("access-value", "refresh-value"));

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertEquals(2, cookies.size());
        // 202609201535 - 验证Access Token和Refresh Token均不会暴露给前端JavaScript。
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("sja_access_token=access-value")
                && value.contains("Path=/api") && value.contains("Secure")
                && value.contains("HttpOnly") && value.contains("SameSite=Strict")));
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith("sja_refresh_token=refresh-value")
                && value.contains("Path=/api/v1/auth") && value.contains("Secure")
                && value.contains("HttpOnly") && value.contains("SameSite=Strict")));
    }
}
