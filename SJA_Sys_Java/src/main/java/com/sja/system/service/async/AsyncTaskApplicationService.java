package com.sja.system.service.async;

import com.sja.system.application.vo.async.AsyncTaskStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 异步任务状态查询用例，屏蔽 Controller 对具体队列实现的感知。
 */
@Service
@RequiredArgsConstructor
// 202609211059 - 完成了异步任务Controller与Redis队列实现的解耦。
public class AsyncTaskApplicationService {

    private final AsyncTaskQueueService asyncTaskQueueService;

    public AsyncTaskStatusVO status(String taskId, String statusToken) {
        return asyncTaskQueueService.getStatus(taskId, statusToken);
    }
}
