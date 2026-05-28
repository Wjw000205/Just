package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("industry_classification")
@Schema(name = "IndustryClassificationEntity", description = "工业战略性新兴产业分类实体")
public class IndustryClassificationEntity {

    @TableId("id")
    @Schema(description = "分类唯一标识")
    private String id;

    @TableField("name")
    @Schema(description = "分类名称")
    private String name;

    @TableField("parent_id")
    @Schema(description = "父级分类 ID，null 代表顶级分类")
    private String parentId;

    @TableField("level")
    @Schema(description = "层级，从 1 开始")
    private Integer level;

    @TableField("sort_order")
    @Schema(description = "排序顺序")
    private Integer sortOrder;
}
