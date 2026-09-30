package com.sja.system.service.storage;

import com.sja.system.application.dto.system.StorageSettingUpdateDTO;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.ApiException;
import com.sja.system.service.authorization.RoleAuthorizationPolicy;
import com.sja.system.service.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 存储设置用例：密码复核和目录变更的调用顺序只在应用层编排。 */
// 202609301034 - 完成了存储路径管理员、秘书长授权及密码二次确认任务。
@Service
public class StorageApplicationService {

    private final StorageLocationService locations;
    private final UserService userService;

    public StorageApplicationService(StorageLocationService locations, UserService userService) {
        this.locations = locations;
        this.userService = userService;
    }

    public List<StorageLocationService.Setting> settings(CurrentActor actor) {
        requireStorageManager(actor);
        return locations.settings();
    }

    public StorageLocationService.Setting check(String category, String relativeDir, CurrentActor actor) {
        requireStorageManager(actor);
        return locations.check(category, relativeDir);
    }

    public StorageLocationService.Setting update(StorageSettingUpdateDTO dto, CurrentActor actor) {
        requireStorageManager(actor);
        userService.verifyCurrentPassword(actor.userId(), dto.currentPassword());
        return locations.update(dto.category(), dto.relativeDir(), actor);
    }

    private static void requireStorageManager(CurrentActor actor) {
        if (actor == null || !RoleAuthorizationPolicy.canManageStorage(actor.roleCode())) {
            throw ApiException.forbidden("只有管理员和秘书长可以查看、配置文件保存路径");
        }
    }
}
