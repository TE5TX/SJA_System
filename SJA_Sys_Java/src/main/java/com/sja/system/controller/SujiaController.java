package com.sja.system.controller;

import com.sja.system.application.model.SujiaExportFile;
import com.sja.system.common.ApiException;
import com.sja.system.common.Result;
import com.sja.system.dto.SujiaResolveDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.SujiaService;
import com.sja.system.vo.SujiaUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tools/sujia")
@RequiredArgsConstructor
public class SujiaController {

    private final SujiaService sujiaService;
    private final SecurityUtils securityUtils;

    @PostMapping("/resolve")
    @PreAuthorize("hasAuthority('tools:sujia:use')")
    public Result<List<SujiaUserVO>> resolve(@RequestBody @Valid SujiaResolveDTO dto,
                                             HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        return Result.success(sujiaService.resolveNames(dto.getNames(), operatorId, roleCode));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('tools:sujia:use')")
    public ResponseEntity<byte[]> export(@RequestBody @Valid SujiaResolveDTO dto,
                                         HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            throw ApiException.unauthorized("未授权");
        }

        // 202609101605 - 完成了素拓Controller只负责请求与文件响应转换的任务。
        SujiaExportFile file = sujiaService.export(dto.getNames(), operatorId, roleCode);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}
