package com.sja.system.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Server-owned import snapshot bound to the operator that created it. */
// 202608210907 - 增加了保存在Redis中的一次性批量注册快照。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportSnapshot {

    private Long operatorId;
    private Integer operatorRoleCode;
    private List<Long> deptIds;
    private List<Long> roleIds;
    private List<UserImportCandidateDTO> candidates;
}
