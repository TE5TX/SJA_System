package com.sja.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sja.system.dal.dataobject.SysRecruitmentForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRecruitmentFormMapper extends BaseMapper<SysRecruitmentForm> {

    // 202609010054 - 共享锁允许并发报名，同时保证暂停操作与正在提交的报名互斥。
    @Select("SELECT * FROM sys_recruitment_form WHERE id = 1 FOR SHARE")
    SysRecruitmentForm selectRegistrationStateForShare();

    @Select("SELECT * FROM sys_recruitment_form WHERE id = 1 FOR UPDATE")
    SysRecruitmentForm selectRegistrationStateForUpdate();
}
