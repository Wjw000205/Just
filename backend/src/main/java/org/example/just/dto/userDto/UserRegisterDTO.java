package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserRegisterDTO", description = "用户注册请求参数")
public class UserRegisterDTO {

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "手机号", example = "13800138000")
    private String telephone;

    @Schema(description = "邮箱", example = "zhangsan@qq.com")
    private String email;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;
}