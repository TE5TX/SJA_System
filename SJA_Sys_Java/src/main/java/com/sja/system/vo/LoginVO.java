package com.sja.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {

    private String token;

    private SessionUserVO userInfo;

    private List<MenuTreeVO> menus;

    private Boolean mustChangePassword;

}
