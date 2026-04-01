package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AuthProfileVO", description = "认证用户信息返回对象")
public class AuthProfileVO {

    @Schema(description = "用户 ID", example = "1")
    private Integer userId;

    @Schema(description = "用户名", example = "张三")
    private String name;

    @Schema(description = "角色", example = "ADMIN")
    private String role;
}
