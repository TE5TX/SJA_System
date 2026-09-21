package com.sja.system.controller;

import com.sja.system.common.Result;
import com.sja.system.service.AsyncTaskApplicationService;
import com.sja.system.vo.AsyncTaskStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户异步任务状态查询接口。
 */
@RestController
@RequestMapping("/api/v1/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskApplicationService asyncTaskApplicationService;

    @GetMapping("/{taskId}")
    public Result<AsyncTaskStatusVO> getStatus(@PathVariable String taskId,
                                               @RequestHeader("X-Task-Token") String statusToken) {
        // 202609211059 - 完成了异步任务Controller对具体Redis队列服务的解耦。
        return Result.success(asyncTaskApplicationService.status(taskId, statusToken));
    }
}
