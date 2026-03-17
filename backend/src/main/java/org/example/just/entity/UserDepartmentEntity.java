package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("user_department")
@Schema(name = "UserDepartmentEntity", description = "用户部门申请/归属实体")
public class UserDepartmentEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("username")
    private String username;

    @TableField("department")
    private String department;

    @TableField("user_state")
    private Integer userState;
}