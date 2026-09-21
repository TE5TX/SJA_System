package com.sja.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sja.system.dal.dataobject.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

}
