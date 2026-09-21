package com.sja.system.application.dto.system;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class OperationLogQueryDTO {

    @Size(max = 50)
    private String realName;

    @Size(max = 50)
    private String operationType;

    @Size(max = 50)
    private String targetModule;

    @Size(max = 20)
    private String resultStatus;

    @Size(max = 64)
    private String requestId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(200)
    private Integer pageSize = 10;
}
