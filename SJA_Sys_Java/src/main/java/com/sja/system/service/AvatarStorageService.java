package com.sja.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AvatarStorageService {

    private static final String URL_PREFIX = "/api/v1/upload/avatar/";
    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-fA-F-]{36}\\.(jpg|png)$");
    private static final Set<String> SUPPORTED_FORMATS = Set.of("JPEG", "JPG", "PNG", "GIF");

    private final Path avatarDirectory;
    private final SysUserMapper sysUserMapper;
    private final long maxPixels;
    private final int maxDimension;
    private final int outputDimension;

    public AvatarStorageService(
            SysUserMapper sysUserMapper,
            @Value("${sja.upload.avatar-dir}") String avatarDirectory,
            @Value("${sja.upload.max-pixels}") long maxPixels,
            @Value("${sja.upload.max-dimension}") int maxDimension,
            @Value("${sja.upload.output-dimension}") int outputDimension) {
        this.sysUserMapper = sysUserMapper;
        this.avatarDirectory = Path.of(avatarDirectory).toAbsolutePath().normalize();
        this.maxPixels = maxPixels;
        this.maxDimension = maxDimension;
        this.outputDimension = outputDimension;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("请选择要上传的照片");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw ApiException.badRequest("照片大小不能超过 5MB");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw ApiException.badRequest("无法读取图片内容");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw ApiException.badRequest("文件不是真实的图片");
            }

            ImageReader reader = readers.next();
            BufferedImage decoded;
            String format;
            try {
                reader.setInput(input, true, true);
                format = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!SUPPORTED_FORMATS.contains(format)) {
                    throw ApiException.badRequest("仅支持 JPEG、PNG 和 GIF 图片");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                decoded = reader.read(0);
            } finally {
                reader.dispose();
            }

            if (decoded == null) {
                throw ApiException.badRequest("图片内容无法解码");
            }
            BufferedImage sanitized = resizeAndStripMetadata(decoded);
            boolean hasAlpha = sanitized.getColorModel().hasAlpha();
            String extension = hasAlpha ? "png" : "jpg";
            String outputFormat = hasAlpha ? "PNG" : "JPEG";
            String filename = UUID.randomUUID() + "." + extension;

            Files.createDirectories(avatarDirectory);
            Path temporary = Files.createTempFile(avatarDirectory, "avatar-", ".tmp");
            try {
                if (!ImageIO.write(sanitized, outputFormat, temporary.toFile())) {
                    throw new IOException("No ImageIO writer for " + outputFormat);
                }
                Files.move(temporary, resolveFile(filename),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return URL_PREFIX + filename;
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            log.warn("Avatar validation or storage failed", e);
            throw ApiException.badRequest("照片内容无效或保存失败");
        }
    }

    public Path resolveUrl(String photoUrl) {
        if (!isManagedUrl(photoUrl)) {
            throw ApiException.badRequest("照片路径不合法");
        }
        return resolveFile(photoUrl.substring(URL_PREFIX.length()));
    }

    public Path resolveFilename(String filename) {
        if (filename == null || !FILE_NAME.matcher(filename).matches()) {
            throw ApiException.notFound("照片不存在");
        }
        return resolveFile(filename);
    }

    public Path resolveReferencedFilename(String filename) {
        // 202608140858 - 阻止通过猜测文件名读取未绑定到任何用户的头像。
        Path path = resolveFilename(filename);
        if (!Files.isRegularFile(path)) {
            throw ApiException.notFound("照片不存在");
        }
        String photoUrl = URL_PREFIX + filename;
        Long references = sysUserMapper.selectCount(new QueryWrapper<SysUser>().eq("photo", photoUrl));
        if (references == null || references < 1) {
            throw ApiException.notFound("照片不存在");
        }
        return path;
    }

    public boolean isManagedUrl(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(URL_PREFIX)) return false;
        return FILE_NAME.matcher(photoUrl.substring(URL_PREFIX.length())).matches();
    }

    public void validateStoredAvatar(String photoUrl) {
        if (!isManagedUrl(photoUrl) || !Files.isRegularFile(resolveUrl(photoUrl))) {
            throw ApiException.badRequest("照片不存在或路径不合法，请重新上传");
        }
    }

    public void validateAssignableAvatar(String photoUrl, Long excludedUserId) {
        validateStoredAvatar(photoUrl);
        QueryWrapper<SysUser> query = new QueryWrapper<SysUser>().eq("photo", photoUrl);
        if (excludedUserId != null) {
            query.ne("id", excludedUserId);
        }
        Long references = sysUserMapper.selectCount(query);
        if (references != null && references > 0) {
            throw ApiException.conflict("该照片已被其他用户使用，请重新上传");
        }
    }

    public void deleteAfterRollback(String photoUrl) {
        if (!isManagedUrl(photoUrl) || !TransactionSynchronizationManager.isActualTransactionActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteIfUnreferenced(photoUrl);
                }
            }
        });
    }

    public void deleteAfterCommit(String photoUrl) {
        if (!isManagedUrl(photoUrl)) return;
        Runnable delete = () -> deleteIfUnreferenced(photoUrl);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete.run();
                }
            });
        } else {
            delete.run();
        }
    }

    private void deleteIfUnreferenced(String photoUrl) {
        try {
            Long referenceCount = sysUserMapper.selectCount(new QueryWrapper<SysUser>()
                    .eq("photo", photoUrl));
            if (referenceCount != null && referenceCount > 0) {
                log.warn("Skipped deletion of referenced avatar: {}", photoUrl);
                return;
            }
            Files.deleteIfExists(resolveUrl(photoUrl));
        } catch (Exception e) {
            log.error("Failed to delete obsolete avatar: {}", photoUrl, e);
        }
    }

    private Path resolveFile(String filename) {
        Path target = avatarDirectory.resolve(filename).normalize();
        if (!target.startsWith(avatarDirectory)) {
            throw ApiException.badRequest("照片路径不合法");
        }
        return target;
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > maxDimension || height > maxDimension
                || (long) width * height > maxPixels) {
            throw ApiException.badRequest("图片像素或尺寸超过限制");
        }
    }

    private BufferedImage resizeAndStripMetadata(BufferedImage source) {
        double scale = Math.min(1D, Math.min(
                (double) outputDimension / source.getWidth(),
                (double) outputDimension / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage output = new BufferedImage(width, height, type);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }
}
