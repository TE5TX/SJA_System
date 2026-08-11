package com.sja.system.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class FeeBatchAuditDTO {

    @NotEmpty(message = "请选择至少一个稿费任务")
    @Size(max = 200, message = "单次最多审核200个稿费任务")
    private List<@Valid FeeTaskRefDTO> tasks;

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Size(max = 500, message = "审核意见不能超过500字")
    private String opinion;
}
