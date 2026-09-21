package com.sja.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已完成任务的数据库幂等标记。
 */
@Data
@TableName("sys_async_task_execution")
public class SysAsyncTaskExecution {

    @TableId(type = IdType.INPUT)
    private String taskId;

    private String taskType;

    private Long ownerId;

    private LocalDateTime completedAt;
}
