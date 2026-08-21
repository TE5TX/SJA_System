package com.sja.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** A normalized Excel row and all validation messages shown in the preview. */
// 202608210907 - 增加了批量注册逐行预览和错误展示模型。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportRowVO {

    private Integer rowNumber;
    private String realName;
    private String studentNo;
    private List<String> deptNames;
    private List<String> roleNames;
    private Boolean valid;
    private List<String> errors;
}
