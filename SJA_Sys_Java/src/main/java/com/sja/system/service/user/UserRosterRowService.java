package com.sja.system.service.user;

import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.application.dto.user.UserRosterSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Creates one roster user in a transaction independent of the other rows. */
// 202609181147 - 完成了人员名单逐行独立事务并复用现有人员创建流程。
@Service
@RequiredArgsConstructor
public class UserRosterRowService {

    private final UserService userService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long createCandidate(UserRosterSnapshot.Candidate candidate, Long roleId, Long operatorId,
                                String operatorName, Integer operatorRoleCode) {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setRealName(candidate.realName());
        dto.setStudentNo(candidate.studentNo());
        dto.setCollege(candidate.college());
        dto.setClassName(candidate.className());
        dto.setPhone(candidate.phone());
        dto.setIdCard(candidate.idCard());
        dto.setDormAddress(candidate.dormAddress());
        dto.setBankCard(candidate.bankCard());
        dto.setDeptIds(candidate.deptIds());
        // 202609181445 - 仅使用导入前选定的统一职务，不再从名单行叠加成员身份。
        dto.setRoleIds(List.of(roleId));
        dto.setIsValid(1);
        // UserService owns the existing initial-password rule; the import requires a change on first login.
        dto.setRequirePasswordChange(true);
        return userService.createUser(dto, operatorId, operatorName, String.valueOf(operatorRoleCode));
    }
}
