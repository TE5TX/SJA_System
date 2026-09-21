package com.sja.system.application.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Validated import preview. A token is issued only when every row is valid. */
// 202608210907 - 增加了批量注册预览汇总和一次性确认令牌响应。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportPreviewVO {

    private String importToken;
    private Long expiresIn;
    private Integer total;
    private Integer validCount;
    private Integer invalidCount;
    private List<UserImportRowVO> rows;
}
