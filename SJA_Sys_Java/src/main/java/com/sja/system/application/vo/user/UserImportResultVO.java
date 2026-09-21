package com.sja.system.application.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Result returned after an all-or-nothing batch registration. */
// 202608210907 - 增加了批量注册成功数量和新用户ID响应。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportResultVO {

    private Integer importedCount;
    private List<Long> userIds;
}
