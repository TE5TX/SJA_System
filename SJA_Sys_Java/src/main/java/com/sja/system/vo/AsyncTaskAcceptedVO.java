package com.sja.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写入请求成功入队后的响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskAcceptedVO {

    private String taskId;
    private String status;
    private String statusUrl;
    private String statusToken;
}
