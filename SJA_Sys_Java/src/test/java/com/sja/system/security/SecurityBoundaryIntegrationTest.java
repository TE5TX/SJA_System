package com.sja.system.security;

import com.sja.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
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
    void limitsBatchRegistrationToAdminChairmanAndSecretary() throws Exception {
        // 202608210907 - 验证批量注册模板接口只允许管理员、主席和秘书长访问。
        for (int roleCode : new int[]{0, 1, 3}) {
            mockMvc.perform(get("/api/v1/users/import/template")
                            .with(authentication(authenticationForRole(roleCode))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/users/import/template")
                        .with(authentication(authenticationForRole(13))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken authenticationForRole(int roleCode) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleCode);
        SjaUserPrincipal principal = new SjaUserPrincipal(99L, "security-test", "", roleCode, 0,
                java.util.List.of(authority));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
