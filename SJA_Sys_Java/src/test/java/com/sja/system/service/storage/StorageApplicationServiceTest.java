package com.sja.system.service.storage;

import com.sja.system.application.dto.system.StorageSettingUpdateDTO;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.ApiException;
import com.sja.system.dal.mysql.StorageLocationRepository;
import com.sja.system.dal.mysql.StorageLocationRepository.Location;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 保存路径用例的权限、密码复核及文件系统写入边界测试。 */
// 202609301034 - 完成了管理员和秘书长可配置、其他角色拒绝及错误密码不写入目录的验证。
class StorageApplicationServiceTest {

    @TempDir
    Path root;

    private final StorageLocationRepository repository = mock(StorageLocationRepository.class);
    private final OperationLogService logs = mock(OperationLogService.class);
    private final UserService users = mock(UserService.class);

    private StorageApplicationService application() {
        return new StorageApplicationService(new StorageLocationService(repository, logs, root.toString()), users);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    void managersCanReadCheckAndUpdateAfterPasswordVerification(int roleCode) {
        when(repository.findByCategory("AVATAR")).thenReturn(List.of(
                new Location(1, "AVATAR", "avatars", true, null, LocalDateTime.now())));
        when(repository.findByCategory("IMPORT")).thenReturn(List.of(
                new Location(2, "IMPORT", "imports", true, null, LocalDateTime.now())));
        CurrentActor actor = new CurrentActor(10L, "存储管理员", roleCode);
        StorageApplicationService application = application();

        assertEquals(2, application.settings(actor).size());
        assertEquals(root.resolve("imports/2026").toString(),
                application.check("IMPORT", "imports/2026", actor).resolvedPath());
        assertEquals(root.resolve("photos/new").toString(), application.update(
                new StorageSettingUpdateDTO("AVATAR", "photos/new", "current-password"), actor).resolvedPath());

        var calls = inOrder(users, repository);
        calls.verify(users).verifyCurrentPassword(10L, "current-password");
        calls.verify(repository).activate("AVATAR", "photos/new", 10L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 99})
    void otherRolesCannotReadCheckOrUpdate(int roleCode) {
        CurrentActor actor = new CurrentActor(10L, "其他角色", roleCode);
        StorageApplicationService application = application();

        assertEquals(403, assertThrows(ApiException.class, () -> application.settings(actor)).getStatus());
        assertEquals(403, assertThrows(ApiException.class,
                () -> application.check("AVATAR", "new", actor)).getStatus());
        assertEquals(403, assertThrows(ApiException.class, () -> application.update(
                new StorageSettingUpdateDTO("AVATAR", "new", "password"), actor)).getStatus());
        assertFalse(Files.exists(root.resolve("new")));
        verifyNoInteractions(users, repository, logs);
    }

    @Test
    void wrongPasswordPreventsDirectoryCreationAndActivation() {
        doThrow(ApiException.badRequest("当前密码错误")).when(users).verifyCurrentPassword(10L, "wrong-password");

        assertThrows(ApiException.class, () -> application().update(
                new StorageSettingUpdateDTO("AVATAR", "new", "wrong-password"),
                new CurrentActor(10L, "秘书长", 3)));

        assertFalse(Files.exists(root.resolve("new")));
        verifyNoInteractions(repository, logs);
    }
}
