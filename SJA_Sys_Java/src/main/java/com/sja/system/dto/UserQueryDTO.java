package com.sja.system.dto;

import lombok.Data;

@Data
public class UserQueryDTO {

    private String username;

    private String realName;

    private String studentNo;

    private String college;

    private String deptCode;

    private Integer roleCode;

    private Integer isValid;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

}
