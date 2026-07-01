package com.sja.system.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class FeeSubmitDTO {

    @NotBlank(message = "作品类型不能为空")
    private String workType;

    @NotNull(message = "作品信息不能为空")
    private Map<String, Object> workInfo;

    @NotEmpty(message = "贡献者列表不能为空")
    @Valid
    private List<ContributorDTO> contributors;

    @Data
    public static class ContributorDTO {

        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @NotBlank(message = "在作品中的角色不能为空")
        private String roleInWork;

        private BigDecimal feeAmount;

        private String remark;

    }

}
