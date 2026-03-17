package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserProfileVO", description = "我的资料返回对象")
public class UserProfileVO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String telephone;

    @Schema(description = "权限")
    private Integer role;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "申请部门")
    private String applyDepartment;

    @Schema(description = "申请状态")
    private String applyState;
}