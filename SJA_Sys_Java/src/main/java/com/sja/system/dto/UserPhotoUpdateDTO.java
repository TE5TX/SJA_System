package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UserPhotoUpdateDTO {

    @NotBlank(message = "照片路径不能为空")
    @Pattern(regexp = "^/api/v1/upload/avatar/[0-9a-fA-F-]{36}\\.(jpg|png)$", message = "照片路径不合法")
    private String photo;
}
