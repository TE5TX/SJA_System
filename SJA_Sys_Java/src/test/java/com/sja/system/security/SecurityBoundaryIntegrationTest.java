package com.sja.system.security;

import com.sja.system.mapper.SysUserMapper;
import com.sja.system.service.LoginCaptchaService;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserRosterImportService;
import com.sja.system.service.RecruitmentService;
import com.sja.system.vo.RecruitmentStatusVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    private RecruitmentService recruitmentService;

    @MockitoBean
    private LoginCaptchaService loginCaptchaService;

    @MockitoBean
    private OperationLogService operationLogService;

    @MockitoBean
    private UserRosterImportService userRosterImportService;

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
        // 202609181028 - 验证新旧模板接口均只允许有批量导入权限的身份访问。
        for (String path : new String[]{"/api/v1/users/import/template",
                "/api/v1/users/import/roster/template"}) {
            for (int roleCode : new int[]{0, 1, 3}) {
                mockMvc.perform(get(path).with(authentication(authenticationForRole(roleCode))))
                        .andExpect(status().isOk());
            }
            mockMvc.perform(get(path).with(authentication(authenticationForRole(13))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void protectsRosterPreviewFromUnauthorizedRoles() throws Exception {
        // 202609181445 - 验证九列名单预览沿用管理员、主席和秘书长的权限边界。
        MockMultipartFile file = new MockMultipartFile("file", "名单.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        for (int roleCode : new int[]{0, 1, 3}) {
            mockMvc.perform(multipart("/api/v1/users/import/roster/preview").file(file)
                            .param("roleId", "5")
                            .with(authentication(authenticationForRole(roleCode))))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(multipart("/api/v1/users/import/roster/preview").file(file)
                        .param("roleId", "5")
                        .with(authentication(authenticationForRole(13))))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/v1/users/import/roster/preview").file(file)
                        .param("roleId", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void limitsRosterConfirmationToImportOperators() throws Exception {
        // 202609181147 - 验证确认接口只允许管理员、主席和秘书长使用。
        String body = "{\"importToken\":\"abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG\","
                + "\"currentPassword\":\"current-password\"}";
        for (int roleCode : new int[]{0, 1, 3}) {
            mockMvc.perform(post("/api/v1/users/import/roster/confirm")
                            .contentType("application/json").content(body)
                            .with(authentication(authenticationForRole(roleCode))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/users/import/roster/confirm")
                        .contentType("application/json").content(body)
                        .with(authentication(authenticationForRole(13))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users/import/roster/confirm")
                        .contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsTaskStatusRouteToUseItsDedicatedToken() throws Exception {
        // 缺少 X-Task-Token 应由参数校验返回 400；若安全规则错误则会提前返回 401。
        mockMvc.perform(get("/api/v1/async-tasks/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesRecruitmentStatusButProtectsPublicationControl() throws Exception {
        // 202609010036 - 公开报名页可读取状态，发布控制仅限管理员、主席和秘书长。
        when(recruitmentService.getStatus()).thenReturn(new RecruitmentStatusVO());
        mockMvc.perform(get("/api/v1/recruitment/status"))
                .andExpect(status().isOk());

        for (int roleCode : new int[]{0, 1, 3}) {
            mockMvc.perform(put("/api/v1/recruitment/manage/status")
                            .contentType("application/json")
                            .content("{\"published\":true}")
                            .with(authentication(authenticationForRole(roleCode))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(put("/api/v1/recruitment/manage/status")
                        .contentType("application/json")
                        .content("{\"published\":true}")
                        .with(authentication(authenticationForRole(4))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAnonymousRecruitmentSubmissionThroughSecurityBoundary() throws Exception {
        String json = """
                {"realName":"测试新生","studentNo":"202600000001","gender":1,
                 "password":"Test123456","confirmPassword":"Test123456",
                 "phone":"13800000000","idCard":"440000200001010001",
                 "college":"计算机学院","className":"26计科A班","dormAddress":"1栋101",
                 "deptCode":"EDITORIAL",
                 "captchaToken":"captcha-token"}
                """;
        MockMultipartFile data = new MockMultipartFile(
                "data", "data.json", "application/json", json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(loginCaptchaService.consume("captcha-token")).thenReturn(true);

        mockMvc.perform(multipart("/api/v1/recruitment/register").file(data))
                .andExpect(status().isOk());
    }

    private UsernamePasswordAuthenticationToken authenticationForRole(int roleCode) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleCode);
        SjaUserPrincipal principal = new SjaUserPrincipal(99L, "security-test", "", roleCode, 0,
                java.util.List.of(authority));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
