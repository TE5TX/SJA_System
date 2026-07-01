package com.sja.system.vo;

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

    private String studentNo;

    private String college;

    private String className;

    private String dormAddress;

    private String phone;

    private String bankCard;

    private Integer isValid;

    private String remark;

    private List<String> roleNames;

    private List<String> deptNames;

    private LocalDateTime createTime;

}
