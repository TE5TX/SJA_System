package com.sja.system.service.storage;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.ApiException;
import com.sja.system.dal.mysql.StorageLocationRepository;
import com.sja.system.dal.mysql.StorageLocationRepository.Location;
import com.sja.system.service.authorization.RoleAuthorizationPolicy;
import com.sja.system.service.system.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 统一限制上传文件只能写入运行环境预先授权的存储根目录。 */
// 202609291708 - 完成了Windows与Linux共用的后台可配置存储路径校验任务。
@Service
public class StorageLocationService {

    public static final String AVATAR = "AVATAR";
    public static final String IMPORT = "IMPORT";

    private final StorageLocationRepository repository;
    private final OperationLogService operationLogService;
    private final Path root;

    @Autowired
    public StorageLocationService(StorageLocationRepository repository,
                                  OperationLogService operationLogService,
                                  @Value("${sja.storage.root:}") String configuredRoot,
                                  @Value("${SJA_AVATAR_DIR:}") String legacyAvatarDirectory) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        String fallback = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "C:/ProgramData/SJA-System/uploads" : "/var/lib/sja-system/uploads";
        // 202609291708 - 兼容旧版镜像Compose：未设置新变量时沿用已挂载的头像父目录。
        String selected = configuredRoot;
        if (selected == null || selected.isBlank()) {
            selected = legacyAvatarDirectory == null || legacyAvatarDirectory.isBlank()
                    ? fallback : Path.of(legacyAvatarDirectory).toAbsolutePath().normalize().getParent().toString();
        }
        this.root = Path.of(selected).toAbsolutePath().normalize();
    }

    /** 便于跨平台目录单元测试直接指定临时根目录。 */
    public StorageLocationService(StorageLocationRepository repository,
                                  OperationLogService operationLogService, String configuredRoot) {
        this(repository, operationLogService, configuredRoot, "");
    }

    // 202609301200 - 为管理页提供可编辑的完整路径及其受限根目录，避免前端猜测Windows或Linux路径。
    public record Setting(String category, String relativeDir, String resolvedPath, String rootPath) { }

    public List<Setting> settings() {
        return List.of(setting(AVATAR), setting(IMPORT));
    }

    public Setting setting(String category) {
        Location location = active(category);
        return new Setting(category, location.relativeDir(), resolve(location.relativeDir(), false).toString(), root.toString());
    }

    public Location active(String category) {
        return repository.findByCategory(category).stream().filter(Location::active).findFirst()
                .orElseThrow(() -> ApiException.serviceUnavailable("文件存储位置尚未初始化，请先执行数据库升级脚本"));
    }

    public Path writableDirectory(String category) {
        return resolve(active(category).relativeDir(), true);
    }

    public Path directory(Location location) {
        return resolve(location.relativeDir(), false);
    }

    public Location location(long id) {
        Location location = repository.findById(id);
        if (location == null) throw ApiException.notFound("文件存储位置不存在");
        return location;
    }

    /** 头像读取遍历历史位置；修改目录只影响后续写入。 */
    public List<Path> readableDirectories(String category) {
        List<Path> directories = new ArrayList<>();
        for (Location location : repository.findByCategory(category)) {
            directories.add(directory(location));
        }
        return directories;
    }

    public Setting check(String category, String relativeDir) {
        requireCategory(category);
        Path directory = resolve(relativeDir, true);
        if (!Files.isWritable(directory)) throw ApiException.badRequest("目录不可写，请检查服务进程权限");
        return new Setting(category, relativeDir, directory.toString(), root.toString());
    }

    @Transactional
    public Setting update(String category, String relativeDir, CurrentActor actor) {
        // 202609301034 - 修复了目录变更服务仍拒绝秘书长的问题，所有入口共用同一权限策略。
        if (actor == null || !RoleAuthorizationPolicy.canManageStorage(actor.roleCode())) {
            throw ApiException.forbidden("只有管理员和秘书长可以配置文件保存路径");
        }
        Setting checked = check(category, relativeDir);
        String old = setting(category).relativeDir();
        if (old.equals(relativeDir)) return checked;
        repository.activate(category, relativeDir, actor.userId());
        operationLogService.log(actor.userId(), actor.username(), actor.roleText(), "UPDATE",
                "SYSTEM", null, "修改文件存储目录：" + category + " [" + old + "] -> [" + relativeDir + "]", "");
        return checked;
    }

    private static void requireCategory(String category) {
        if (!AVATAR.equals(category) && !IMPORT.equals(category)) {
            throw ApiException.badRequest("不支持的文件类别");
        }
    }

    private Path resolve(String relativeDir, boolean create) {
        if (relativeDir == null || relativeDir.isBlank() || relativeDir.length() > 200
                || relativeDir.contains("\\") || relativeDir.contains(":") || relativeDir.startsWith("/")
                || !relativeDir.matches("[\\p{L}\\p{N}_/-]+")) {
            throw ApiException.badRequest("目录只能填写存储根目录下的相对子目录");
        }
        for (String segment : relativeDir.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw ApiException.badRequest("目录不能包含空段或上级目录");
            }
        }
        Path directory = root.resolve(relativeDir).normalize();
        if (!directory.startsWith(root)) throw ApiException.badRequest("目录超出允许的存储范围");
        try {
            // 202609291708 - 创建目录前逐级拒绝符号链接，避免先写入再检测造成越界。
            Path current = root.getRoot();
            for (Path part : root) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) throw ApiException.badRequest("存储根目录不能包含符号链接");
            }
            current = root;
            for (String part : relativeDir.split("/")) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) throw ApiException.badRequest("目录不能包含符号链接");
            }
            if (create) Files.createDirectories(directory);
            if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                    throw ApiException.badRequest("目标不是普通目录");
                }
                if (!directory.toRealPath().startsWith(root.toRealPath())) {
                    throw ApiException.badRequest("目录指向存储根目录之外");
                }
            }
            return directory;
        } catch (IOException e) {
            throw ApiException.badRequest("目录不可用，请检查挂载及服务进程权限");
        }
    }
}
