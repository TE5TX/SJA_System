package com.sja.system.controller.upload;

import com.sja.system.application.model.AvatarFile;
import com.sja.system.common.Result;
import com.sja.system.framework.web.MultipartUploadedFile;
import com.sja.system.service.upload.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.util.Map;

/**
 * 头像上传与读取协议适配器。
 */
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
// 202609211059 - 完成了头像读取校验和媒体类型判断从Controller下沉。
public class UploadController {

    private final AvatarStorageService avatarStorageService;

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 202609211059 - 完成了Web上传对象向应用层文件模型的适配。
        return Result.success(Map.of("url", avatarStorageService.store(new MultipartUploadedFile(file))));
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) throws MalformedURLException {
        // 202609211059 - 完成了头像存在性、引用关系和媒体类型判断从Controller下沉。
        AvatarFile file = avatarStorageService.loadReferenced(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new UrlResource(file.path().toUri()));
    }
}
