package com.sja.system.security;

import com.sja.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "sja.async-task.enabled=false",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "jwt.secret=01234567890123456789012345678901",
        "sja.security.cors-allowed-origins=http://localhost:5173",
        "knife4j.enable=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
class SecurityBoundaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // 202608140858 - 隔离数据库以验证头像认证边界本身。
    @MockitoBean
    private SysUserMapper sysUserMapper;

    @Test
    void allowsConfiguredFrontendPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void rejectsUnconfiguredOriginPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/users")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void protectsApiDocumentationFromAnonymousAccess() throws Exception {
        mockMvc.perform(get("/v2/api-docs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsAvatarFilesFromAnonymousAccess() throws Exception {
        String path = "/api/v1/upload/avatar/11111111-1111-1111-1111-111111111111.png";

        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());

        // Authentication reaches the controller; the test file itself is intentionally absent.
        mockMvc.perform(get(path).with(user("security-test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowsTaskStatusRouteToUseItsDedicatedToken() throws Exception {
        // 缺少 X-Task-Token 应由参数校验返回 400；若安全规则错误则会提前返回 401。
        mockMvc.perform(get("/api/v1/async-tasks/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isBadRequest());
    }
}
