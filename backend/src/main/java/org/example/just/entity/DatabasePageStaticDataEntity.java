package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("database_page_static_data")
@Schema(name = "DatabasePageStaticDataEntity", description = "数据库页面静态数据实体")
public class DatabasePageStaticDataEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Integer id;

    @TableField("parent_id")
    @Schema(description = "父级 ID，顶级节点为 0")
    private Integer parentId;

    @TableField("category_type")
    @Schema(description = "数据类型标识： data_category(数据类别), department(所属部门)")
    private String categoryType;

    @TableField("name")
    @Schema(description = "显示名称")
    private String name;

    @TableField("sort_order")
    @Schema(description = "排序字段")
    private Integer sortOrder;
}
