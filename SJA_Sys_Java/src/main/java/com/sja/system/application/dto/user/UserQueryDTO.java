package com.sja.system.application.dto.user;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Data
public class UserQueryDTO {

    @Size(max = 50)
    private String username;

    @Size(max = 50)
    private String realName;

    @Size(max = 20)
    private String studentNo;

    @Size(max = 100)
    private String college;

    @Size(max = 50)
    private String deptCode;

    private Integer roleCode;

    @Min(0)
    @Max(1)
    private Integer isValid;

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(200)
    private Integer pageSize = 10;

}
