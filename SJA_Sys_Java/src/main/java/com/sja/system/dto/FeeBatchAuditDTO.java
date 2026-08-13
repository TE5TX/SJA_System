package com.sja.system.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "请输入当前用户密码")
    @Size(max = 50, message = "当前用户密码不能超过50个字符")
    private String currentPassword;
}
