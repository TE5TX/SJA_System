package com.sja.system.service.storage;

import com.sja.system.common.ApiException;
import com.sja.system.dal.mysql.StorageLocationRepository;
import com.sja.system.dal.mysql.StorageLocationRepository.Location;
import com.sja.system.service.system.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 跨平台相对目录与历史目录行为测试。 */
// 202609291708 - 覆盖了存储路径越界拒绝和新旧目录并存场景。
class StorageLocationServiceTest {

    @TempDir Path root;

    @Test
    void rejectsAbsoluteAndParentPaths() {
        StorageLocationService service = new StorageLocationService(mock(StorageLocationRepository.class),
                mock(OperationLogService.class), root.toString());
        for (String input : List.of("../outside", "avatars/../outside", "C:/data", "a\\b", "/tmp")) {
            assertThrows(ApiException.class, () -> service.check(StorageLocationService.AVATAR, input));
        }
    }

    @Test
    void resolvesActiveAndHistoricalDirectoriesWithinRoot() {
        StorageLocationRepository repository = mock(StorageLocationRepository.class);
        when(repository.findByCategory("AVATAR")).thenReturn(List.of(
                new Location(2, "AVATAR", "photos/new", true, 1L, LocalDateTime.now()),
                new Location(1, "AVATAR", "avatars", false, null, LocalDateTime.now())));
        StorageLocationService service = new StorageLocationService(repository,
                mock(OperationLogService.class), root.toString());

        assertEquals(root.resolve("photos/new"), service.writableDirectory("AVATAR"));
        assertEquals(2, service.readableDirectories("AVATAR").size());
        assertTrue(service.check("AVATAR", "照片/2026").resolvedPath().endsWith("照片\\2026")
                || service.check("AVATAR", "照片/2026").resolvedPath().endsWith("照片/2026"));
    }
}
