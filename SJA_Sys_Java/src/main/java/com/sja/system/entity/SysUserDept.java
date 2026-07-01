package com.sja.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_user_dept")
public class SysUserDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deptId;

    private String remark;

    private LocalDateTime createTime;

}
