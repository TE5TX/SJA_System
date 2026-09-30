package com.sja.system.service.storage;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.common.ApiException;
import com.sja.system.common.PageResult;
import com.sja.system.dal.mysql.ImportArchiveRepository;
import com.sja.system.dal.mysql.ImportArchiveRepository.Archive;
import com.sja.system.dal.mysql.StorageLocationRepository.Location;
import com.sja.system.service.authorization.RoleAuthorizationPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/** 保存原始导入表；数据库仅保存逻辑位置和随机文件名，不暴露主机绝对路径。 */
// 202609291708 - 完成了两类人员导入原件归档、校验和受控下载任务。
@Slf4j
@Service
public class ImportArchiveService {

    public static final String BASIC = "BASIC";
    public static final String ROSTER = "ROSTER";
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final StorageLocationService locations;
    private final ImportArchiveRepository repository;

    /** 服务层归档摘要，不直接依赖应用层响应对象。 */
    public record Summary(String id, String originalName, String sha256, long fileSize,
                          String importType, String operatorName, String status,
                          int totalCount, int importedCount, int skippedCount,
                          int unprocessedCount, String message,
                          LocalDateTime createdAt, LocalDateTime confirmedAt) { }

    public record ArchivedFile(String fileName, byte[] content) {
        public ArchivedFile { content = content.clone(); }
        @Override public byte[] content() { return content.clone(); }
    }

    public ImportArchiveService(StorageLocationService locations, ImportArchiveRepository repository) {
        this.locations = locations;
        this.repository = repository;
    }

    public void savePreview(UploadedFile file, String token, String type, CurrentActor actor, int total) {
        if (token == null || token.isBlank()) return;
        if (!BASIC.equals(type) && !ROSTER.equals(type)) throw ApiException.badRequest("导入类型不正确");
        if (file.size() > MAX_BYTES || file.empty()) throw ApiException.badRequest("导入文件大小不正确");
        Location location = locations.active(StorageLocationService.IMPORT);
        String id = UUID.randomUUID().toString();
        LocalDate date = LocalDate.now();
        String storedName = date.getYear() + "/" + String.format("%02d", date.getMonthValue())
                + "/" + id + ".xlsx";
        Path directory = locations.writableDirectory(StorageLocationService.IMPORT);
        Path target = directory.resolve(storedName).normalize();
        try {
            Files.createDirectories(target.getParent());
            if (!target.getParent().toRealPath().startsWith(directory.toRealPath())) {
                throw ApiException.badRequest("导入归档目录指向存储根目录之外");
            }
            // 原件使用 CREATE_NEW 写入随机文件名；不接受客户端提供的目录或文件名。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long bytes = 0;
            try (InputStream input = file.inputStream(); var output = Files.newOutputStream(target,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes += count;
                    if (bytes > MAX_BYTES) throw new IOException("导入文件超过大小限制");
                    digest.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
            }
            // Windows使用继承ACL；POSIX系统进一步收紧原件权限为仅所有者读写。
            if (Files.getFileStore(target).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(target, Set.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE));
            }
            Archive archive = new Archive(id, location.id(), storedName,
                    safeOriginalName(file.originalFilename()), HexFormat.of().formatHex(digest.digest()),
                    bytes, type, actor.userId(), actor.username(), "PREVIEWED", total,
                    0, 0, 0, null, null, null);
            repository.insert(archive, tokenHash(token));
        } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
            log.error("无法保存人员导入原件：fileId={}", id, e);
            // 不删除可能已写入的原件，避免误删；运维可根据随机ID排查孤立文件。
            throw ApiException.serviceUnavailable("人员导入原件归档失败，本次预览未生效，请检查存储目录");
        }
    }

    public void finishBasic(String token, CurrentActor actor, int importedCount) {
        Archive archive = requireToken(token, actor, BASIC);
        if (repository.finish(archive.id(), "SUCCESS", importedCount, 0, 0, null) != 1) {
            throw ApiException.conflict("导入归档状态已变化，请查询导入记录");
        }
    }

    public void finishRoster(String token, CurrentActor actor, int importedCount,
                             int skippedCount, int unprocessedCount, boolean aborted, String abortReason) {
        Archive archive = requireToken(token, actor, ROSTER);
        String status = aborted ? "ABORTED" : skippedCount > 0 ? "PARTIAL" : "SUCCESS";
        if (repository.finish(archive.id(), status, importedCount, skippedCount,
                unprocessedCount, abortReason) != 1) {
            throw ApiException.conflict("导入归档状态已变化，请查询导入记录");
        }
    }

    public void requirePreview(String token, CurrentActor actor, String type) {
        Archive archive = requireToken(token, actor, type);
        if (!"PREVIEWED".equals(archive.status())) throw ApiException.conflict("该导入记录已确认");
        verifiedPath(archive);
    }

    /** 确认异常不覆盖原始异常，也不误记为已导入；令牌是否仍可用由现有导入服务决定。 */
    public void noteFailedAttempt(String token, CurrentActor actor, String type, RuntimeException cause) {
        try {
            Archive archive = requireToken(token, actor, type);
            String message = "确认请求失败，结果请核对：" + cause.getMessage();
            repository.noteFailedAttempt(archive.id(), message.length() > 500
                    ? message.substring(0, 500) : message);
        } catch (RuntimeException auditError) {
            log.error("无法记录人员导入确认异常：operatorId={}", actor.userId(), auditError);
        }
    }

    public PageResult<Summary> page(int pageNum, int pageSize, CurrentActor actor) {
        requireImporter(actor);
        int size = Math.max(1, Math.min(pageSize, 50));
        int page = Math.max(1, pageNum);
        return new PageResult<>(repository.page(size, (page - 1) * size).stream().map(this::toView).toList(),
                repository.count(), page, size);
    }

    public ArchivedFile download(String id, CurrentActor actor) {
        requireImporter(actor);
        if (id == null || !id.matches("[0-9a-fA-F-]{36}")) throw ApiException.badRequest("归档编号格式不正确");
        Archive archive = repository.findById(id);
        if (archive == null) throw ApiException.notFound("导入归档不存在");
        Path path = verifiedPath(archive);
        try {
            return new ArchivedFile(archive.originalName(), Files.readAllBytes(path));
        } catch (IOException e) {
            log.error("读取导入归档失败：fileId={}", id, e);
            throw ApiException.serviceUnavailable("导入文件暂不可读取");
        }
    }

    private Archive requireToken(String token, CurrentActor actor, String type) {
        requireImporter(actor);
        if (token == null || token.isBlank()) throw ApiException.badRequest("导入令牌不能为空");
        Archive archive = repository.findByToken(tokenHash(token), actor.userId());
        if (archive == null || !type.equals(archive.importType())) {
            throw ApiException.notFound("导入原件归档不存在，请重新上传预览");
        }
        return archive;
    }

    private Summary toView(Archive archive) {
        return new Summary(archive.id(), archive.originalName(), archive.sha256(),
                archive.fileSize(), archive.importType(), archive.operatorName(), archive.status(),
                archive.totalCount(), archive.importedCount(), archive.skippedCount(),
                archive.unprocessedCount(), archive.message(), archive.createdAt(), archive.confirmedAt());
    }

    private Path verifiedPath(Archive archive) {
        Location location = locations.location(archive.locationId());
        if (!StorageLocationService.IMPORT.equals(location.category())
                || !archive.storedName().matches("\\d{4}/\\d{2}/[0-9a-fA-F-]{36}\\.xlsx")) {
            throw ApiException.badRequest("导入归档路径不合法");
        }
        Path root = locations.directory(location);
        Path target = root.resolve(archive.storedName()).normalize();
        try {
            if (!target.startsWith(root) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                    || !target.toRealPath().startsWith(root.toRealPath())
                    || Files.size(target) != archive.fileSize() || archive.fileSize() > MAX_BYTES) {
                throw ApiException.notFound("导入原件缺失或大小不一致");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(archive.sha256())) {
                throw ApiException.conflict("导入原件校验失败，请联系管理员");
            }
            return target;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw ApiException.serviceUnavailable("无法校验导入原件");
        }
    }

    private static void requireImporter(CurrentActor actor) {
        if (actor == null || !RoleAuthorizationPolicy.canImportUsers(actor.roleCode())) {
            throw ApiException.forbidden("无权查看人员导入归档");
        }
    }

    private static String safeOriginalName(String name) {
        String safe = name == null ? "人员导入.xlsx" : name.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\p{Cntrl}]", "_");
        if (safe.isBlank()) safe = "人员导入.xlsx";
        return safe.length() <= 180 ? safe : safe.substring(0, 175) + ".xlsx";
    }

    private static String tokenHash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM缺少SHA-256", e);
        }
    }
}
