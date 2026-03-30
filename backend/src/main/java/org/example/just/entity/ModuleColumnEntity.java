package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("module_column")
public class ModuleColumnEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("module_id")
    private Integer moduleId;

    @TableField("column_name")
    private String columnName;

    @TableField("type")
    private String type;

    @TableField("belong")
    private String belong;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("deleted")
    private Integer deleted;
}