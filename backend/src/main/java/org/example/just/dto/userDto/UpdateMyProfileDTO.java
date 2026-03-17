package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UpdateMyProfileDTO", description = "编辑我的资料请求参数")
public class UpdateMyProfileDTO {

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "电子邮箱", example = "zhangsan@qq.com")
    private String email;

    @Schema(description = "手机号码", example = "13800138000")
    private String telephone;
}