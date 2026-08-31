package com.sja.system.controller;

import com.sja.system.common.Result;
import com.sja.system.service.AsyncTaskQueueService;
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

    private final AsyncTaskQueueService queueService;

    @GetMapping("/{taskId}")
    public Result<AsyncTaskStatusVO> getStatus(@PathVariable String taskId,
                                               @RequestHeader("X-Task-Token") String statusToken) {
        // 202608192148 - 修复了学号修改使旧JWT失效后无法读取任务最终状态的问题。
        return Result.success(queueService.getStatus(taskId, statusToken));
    }
}
