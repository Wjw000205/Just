package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("module")
@Schema(description = "module表实体")
public class ModuleEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("module_name")
    private String moduleName;

    /**
     * 你的表字段是 Tag，大小写不规则，建议显式指定
     */
    @TableField("Tag")
    private String tag;

    @TableField("description")
    private String description;

    @TableField("creator")
    private String creator;

    @TableField("visible_area")
    private Integer visibleArea;

    @TableField("agree")
    private Integer agree;

    @TableField("deleted")
    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;
}