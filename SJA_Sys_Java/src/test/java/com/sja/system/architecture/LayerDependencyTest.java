package com.sja.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerDependencyTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "com", "sja", "system");

    @Test
    void controllersDoNotImportMappers() throws IOException {
        assertNoImport(MAIN_SOURCE.resolve("controller"), "import com.sja.system.mapper.");
    }

    @Test
    void refactoredControllersDoNotContainPersistenceOrExcelCode() throws IOException {
        Path controllers = MAIN_SOURCE.resolve("controller");
        for (String controller : List.of("AuthController.java", "SujiaController.java")) {
            Path source = controllers.resolve(controller);
            assertNoImport(source, "import com.baomidou.mybatisplus.");
            assertNoImport(source, "import com.alibaba.excel.");
        }
    }

    @Test
    void servicesDoNotImportSecurityImplementations() throws IOException {
        assertNoImport(MAIN_SOURCE.resolve("service"), "import com.sja.system.security.");
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

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取架构检查文件: " + path, exception);
        }
    }
}
