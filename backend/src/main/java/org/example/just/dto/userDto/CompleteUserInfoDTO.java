package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CompleteUserInfoDTO", description = "完善用户信息请求参数")
public class CompleteUserInfoDTO {

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "二级密码", example = "123456")
    private String secondPassword;

    @Schema(description = "确认二级密码", example = "123456")
    private String confirmSecondPassword;
}