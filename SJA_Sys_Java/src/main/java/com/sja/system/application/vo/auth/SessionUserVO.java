package com.sja.system.application.vo.auth;

import lombok.Data;

import java.util.List;

/**
 * Minimal user identity returned by authentication endpoints.
 * Sensitive profile fields are deliberately excluded.
 */
@Data
public class SessionUserVO {

    private Long id;

    private String username;

    private String realName;

    private String photo;

    private Integer isValid;

    private List<String> roleNames;

    private List<Integer> roleCodes;

    private List<String> deptNames;

    private List<String> deptCodes;
}
