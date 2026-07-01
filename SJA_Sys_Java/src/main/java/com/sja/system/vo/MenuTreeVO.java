package com.sja.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuTreeVO {

    private Long id;

    private String menuName;

    private String permissionCode;

    private Integer menuType;

    private Long parentId;

    private String path;

    private Integer sortOrder;

    private List<MenuTreeVO> children;

}
