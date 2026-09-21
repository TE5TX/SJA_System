package com.sja.system.application.dto.tool;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SujiaResolveDTO {

    @NotEmpty(message = "姓名列表不能为空")
    private List<String> names;

}
