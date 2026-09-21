package com.sja.system.controller.tool;

import com.sja.system.application.model.SujiaExportFile;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.Result;
import com.sja.system.framework.web.support.DownloadResponseFactory;
import com.sja.system.application.dto.tool.SujiaResolveDTO;
import com.sja.system.service.tool.SujiaService;
import com.sja.system.application.vo.tool.SujiaUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 素拓名单解析与导出协议适配器。
 */
@RestController
@RequestMapping("/api/v1/tools/sujia")
@RequiredArgsConstructor
// 202609211059 - 完成了素拓Controller身份解析下沉。
public class SujiaController {

    private final SujiaService sujiaService;
    @PostMapping("/resolve")
    @PreAuthorize("hasAuthority('tools:sujia:use')")
    public Result<List<SujiaUserVO>> resolve(@RequestBody @Valid SujiaResolveDTO dto,
                                             CurrentActor actor) {
        return Result.success(sujiaService.resolveNames(dto.getNames(), actor.userId(), actor.roleCode()));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('tools:sujia:use')")
    public ResponseEntity<byte[]> export(@RequestBody @Valid SujiaResolveDTO dto,
                                         CurrentActor actor) {
        // 202609211059 - 完成了素拓Controller身份解析下沉，仅保留文件响应转换。
        SujiaExportFile file = sujiaService.export(dto.getNames(), actor.userId(), actor.roleCode());
        return DownloadResponseFactory.create(file);
    }
}
