package com.sja.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Minimal active-user information used when selecting fee contributors.
 */
@Data
@AllArgsConstructor
public class FeeContributorVO {

    private Long id;
    private String realName;
    private String studentNo;
    private Integer isValid;
    private List<Integer> roleCodes;
}
