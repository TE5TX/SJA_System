package com.sja.system.application.vo.async;

import lombok.Data;

/**
 * 前端轮询使用的最小任务状态，不能包含队列正文或其他用户资料。
 */
@Data
public class AsyncTaskStatusVO {

    private String taskId;
    private String type;
    private String status;
    private String message;
    private Integer attempt;
    private String createdAt;
    private String updatedAt;
}
