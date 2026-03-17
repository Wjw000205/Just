package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UpdateSecondPasswordDTO", description = "修改二级密码请求参数")
public class UpdateSecondPasswordDTO {

    @Schema(description = "当前二级密码", example = "111111")
    private String currentSecondPassword;

    @Schema(description = "新的二级密码", example = "222222")
    private String newSecondPassword;

    @Schema(description = "确认二级密码", example = "222222")
    private String confirmSecondPassword;
}