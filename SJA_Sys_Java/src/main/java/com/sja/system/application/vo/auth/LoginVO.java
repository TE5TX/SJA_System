package com.sja.system.application.vo.auth;

import com.sja.system.application.vo.system.MenuTreeVO;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private SessionUserVO userInfo;

    private List<MenuTreeVO> menus;

    private Boolean mustChangePassword;

}
