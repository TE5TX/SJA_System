package com.sja.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sja.system.dal.dataobject.SysRefreshToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRefreshTokenMapper extends BaseMapper<SysRefreshToken> {
}
