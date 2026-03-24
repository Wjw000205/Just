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
@TableName("module")
@Schema(name = "ModuleEntity", description = "模板目录实体")
public class ModuleEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @TableField("module_name")
    @Schema(description = "模板/目录名称")
    private String moduleName;

    @TableField("creator")
    @Schema(description = "创建人")
    private String creator;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("parent")
    @Schema(description = "父级目录，null 代表一级目录")
    private String parent;

    @TableField("is_menu")
    @Schema(description = "是否目录：1是目录，0不是目录")
    private Integer isMenu;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：0未删除，1已删除")
    private Integer deleted;

    @TableField("audit_status")
    @Schema(description = "审核状态：0未审核，1审核通过，2审核未通过")
    private Integer auditStatus;
}