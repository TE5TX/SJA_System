package com.sja.system.application.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员或秘书长提交的相对存储目录与当前密码。 */
// 202609291708 - 完成了后台存储目录设置请求模型。
public record StorageSettingUpdateDTO(@NotBlank String category,
                                      @NotBlank @Size(max = 200) String relativeDir,
                                      @NotBlank String currentPassword) { }
