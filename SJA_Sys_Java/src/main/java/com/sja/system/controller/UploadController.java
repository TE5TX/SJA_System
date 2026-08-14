package com.sja.system.controller;

import com.sja.system.common.ApiException;
import com.sja.system.common.Result;
import com.sja.system.service.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final AvatarStorageService avatarStorageService;

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(Map.of("url", avatarStorageService.store(file)));
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        // 202608140858 - 头像仅允许认证用户读取，且禁止缓存和读取未绑定文件。
        Path path = avatarStorageService.resolveReferencedFilename(filename);
        if (!Files.isRegularFile(path)) {
            throw ApiException.notFound("照片不存在");
        }
        try {
            MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new UrlResource(path.toUri()));
        } catch (IOException e) {
            throw ApiException.notFound("照片不存在");
        }
    }
}
