package com.sja.system.application.vo.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private Integer gender;

    private String photo;

    private String idCard;

    private String studentNo;

    private String college;

    private String className;

    private String dormAddress;

    private String phone;

    private String bankCard;

    private Integer isValid;

    private Integer mustChangePassword;

    private String remark;

    private List<String> roleNames;

    private List<Long> roleIds;

    private List<Integer> roleCodes;

    private List<String> deptNames;

    private List<Long> deptIds;

    private List<String> deptCodes;

    private LocalDateTime createTime;

}
