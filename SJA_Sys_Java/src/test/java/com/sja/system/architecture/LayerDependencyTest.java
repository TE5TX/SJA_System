package com.sja.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerDependencyTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "com", "sja", "system");

    @Test
    void controllersDoNotImportMappers() throws IOException {
        assertNoImport(MAIN_SOURCE.resolve("controller"), "import com.sja.system.dal.mysql.");
    }

    @Test
    void controllersDoNotParseSecurityIdentityOrCoordinateSensitiveOperations() throws IOException {
        // 202609211059 - 防止Controller再次解析安全主体、直接选择异步队列或编排密码复核。
        Path controllers = MAIN_SOURCE.resolve("controller");
        assertNoImport(controllers, "import com.sja.system.framework.security.SecurityUtils;");
        assertNoImport(controllers, "import com.sja.system.service.async.AsyncTaskQueueService;");
        assertNoPattern(controllers, Pattern.compile("\\.verifyCurrentPassword\\s*\\("));
    }

    @Test
    void refactoredControllersDoNotContainPersistenceOrExcelCode() throws IOException {
        Path controllers = MAIN_SOURCE.resolve("controller");
        // 202609211427 - 完成了Controller按业务域分包后的全目录持久化与Excel依赖检查。
        assertNoImport(controllers, "import com.baomidou.mybatisplus.");
        assertNoImport(controllers, "import com.alibaba.excel.");
    }

    @Test
    void servicesDoNotImportSecurityImplementations() throws IOException {
        assertNoImport(MAIN_SOURCE.resolve("service"), "import com.sja.system.framework.security.");
    }

    @Test
    void servicesDoNotDependOnServletApi() throws IOException {
        // 202609201130 - 防止Service再次依赖Servlet请求或响应对象。
        assertNoImport(MAIN_SOURCE.resolve("service"), "import jakarta.servlet.");
    }

    @Test
    void servicesDoNotDependOnSpringWebUploadTypes() throws IOException {
        // 202609211059 - 防止Service层反向依赖Spring Web上传协议类型。
        assertNoImport(MAIN_SOURCE.resolve("service"), "import org.springframework.web.");
    }

    @Test
    void controllersDoNotBuildDownloadHeadersOrWriteResponseStreamsDirectly() throws IOException {
        // 202609201621 - 强制所有文件下载通过统一HTTP响应工厂处理。
        Path controllers = MAIN_SOURCE.resolve("controller");
        Pattern duplicatedDownloadHandling = Pattern.compile(
                "Content-Disposition|CONTENT_DISPOSITION|getOutputStream\\s*\\(|URLEncoder");
        try (var files = Files.walk(controllers)) {
            List<Path> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> duplicatedDownloadHandling.matcher(read(path)).find())
                    .toList();
            assertTrue(violations.isEmpty(),
                    () -> "Controller不得手工构造下载响应，应使用DownloadResponseFactory: " + violations);
        }
    }

    @Test
    void servicesDoNotDependOnHttpDownloadTypes() throws IOException {
        // 202609201621 - 防止Service层重新依赖HTTP下载响应类型。
        for (String forbidden : List.of(
                "import jakarta.servlet.http.HttpServletResponse;",
                "import org.springframework.http.ContentDisposition;",
                "import org.springframework.http.HttpHeaders;")) {
            assertNoImport(MAIN_SOURCE.resolve("service"), forbidden);
        }
    }

    @Test
    void feeFacadeDoesNotDependOnPersistenceOrImplementationDetails() throws IOException {
        // 202609210900 - 防止稿费门面再次直接访问Mapper或五类作品实体。
        Path facade = MAIN_SOURCE.resolve("service").resolve("fee").resolve("FeeServiceImpl.java");
        assertNoImport(facade, "import com.sja.system.dal.mysql.");
        assertNoImport(facade, "import com.sja.system.dal.dataobject.");
    }

    @Test
    void feeUseCasesDoNotDependOnServiceImplementations() throws IOException {
        // 202609210900 - 防止稿费查询、写入、审核和导出用例反向依赖service.impl包。
        assertNoImport(MAIN_SOURCE.resolve("service").resolve("fee"), "import com.sja.system.service.impl.");
    }

    @Test
    void asyncCoreDoesNotDependOnOuterLayers() throws IOException {
        // 202609201556 - 防止异步队列核心身份模型反向依赖配置、Web或业务实现层。
        Path asyncPackage = MAIN_SOURCE.resolve("service").resolve("async").resolve("model");
        for (String forbidden : List.of(
                "import com.sja.system.framework.config.",
                "import com.sja.system.controller.",
                "import com.sja.system.framework.web.",
                "import com.sja.system.framework.redis.",
                "import com.sja.system.framework.security.",
                "import com.sja.system.service.")) {
            assertNoImport(asyncPackage, forbidden);
        }
    }

    @Test
    void businessEntryPointsAreGroupedByDomain() throws IOException {
        // 202609211427 - 防止Controller和Service再次退化为根目录平铺结构。
        assertNoJavaFileDirectlyUnder(MAIN_SOURCE.resolve("controller"));
        assertNoJavaFileDirectlyUnder(MAIN_SOURCE.resolve("service"));
    }

    @Test
    void servicesDoNotHardCodeRoleNumbers() throws IOException {
        // 202609201146 - 防止角色码数字判断再次散落到各业务服务。
        Pattern magicRoleCode = Pattern.compile(
                "\\b(?:operatorRoleCode|roleCode)\\s*(?:==|!=|<|>)\\s*\\d+"
                        + "|roles\\.contains\\(\\s*\\d+"
                        + "|Set\\.of\\(\\s*\\d+");
        assertNoPattern(MAIN_SOURCE.resolve("service"), magicRoleCode);
        assertNoPattern(MAIN_SOURCE.resolve("controller"), Pattern.compile("['\\\"]ROLE_\\d+"));
    }

    private void assertNoImport(Path packagePath, String forbiddenImport) throws IOException {
        // 202609101605 - 完成了Controller越层访问和Service反向依赖的自动化架构约束。
        try (var files = Files.walk(packagePath)) {
            List<Path> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains(forbiddenImport))
                    .toList();
            assertTrue(violations.isEmpty(),
                    () -> "禁止的层级依赖 " + forbiddenImport + " 出现在: " + violations);
        }
    }

    private void assertNoPattern(Path packagePath, Pattern forbiddenPattern) throws IOException {
        try (var files = Files.walk(packagePath)) {
            List<Path> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> forbiddenPattern.matcher(read(path)).find())
                    .toList();
            assertTrue(violations.isEmpty(),
                    () -> "禁止的代码模式 " + forbiddenPattern + " 出现在: " + violations);
        }
    }

    private void assertNoJavaFileDirectlyUnder(Path packagePath) throws IOException {
        try (var files = Files.list(packagePath)) {
            List<Path> violations = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .toList();
            assertTrue(violations.isEmpty(),
                    () -> "业务入口必须按领域分包，不得直接平铺在根目录: " + violations);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取架构检查文件: " + path, exception);
        }
    }
}
