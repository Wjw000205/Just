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
@TableName("department")
@Schema(name = "DepartmentEntity", description = "部门实体")
public class DepartmentEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @TableField("department_name")
    @Schema(description = "部门名称")
    private String departmentName;

    @TableField("leader")
    @Schema(description = "负责人")
    private String leader;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    @TableField("created_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}