package com.sja.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Normalized row persisted in Redis between preview and confirmation. */
// 202608210907 - 增加了批量注册预览通过后的规范化人员数据结构。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportCandidateDTO {

    private Integer rowNumber;
    private String realName;
    private String studentNo;
}
