package com.sja.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_refresh_token")
public class SysRefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tokenId;

    private String tokenHash;

    private String tokenFamily;

    private Long userId;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private String replacedBy;

    private LocalDateTime createTime;
}
