package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserLoginDTO", description = "用户登录请求参数")
public class UserLoginDTO {

    @Schema(description = "用户名", example = "张三")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;
}
