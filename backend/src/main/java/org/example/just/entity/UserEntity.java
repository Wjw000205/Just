package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
@Schema(name = "UserEntity", description = "用户实体")
public class UserEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @TableField("username")
    @Schema(description = "用户名")
    private String username;

    @TableField("password")
    @Schema(description = "加密后的密码")
    private String password;

    @TableField("telephone")
    @Schema(description = "手机号")
    private String telephone;

    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    @TableField("role")
    @Schema(description = "角色，默认0")
    private Integer role;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    @TableField("real_name")
    @Schema(description = "真实姓名")
    private String realName;

    @TableField("second_password")
    @Schema(description = "二级密码")
    private String secondPassword;
}