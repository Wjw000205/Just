package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UpdatePasswordDTO", description = "修改登录密码请求参数")
public class UpdatePasswordDTO {

    @Schema(description = "当前密码", example = "123456")
    private String currentPassword;

    @Schema(description = "新的登录密码", example = "654321")
    private String newPassword;

    @Schema(description = "确认密码", example = "654321")
    private String confirmPassword;
}