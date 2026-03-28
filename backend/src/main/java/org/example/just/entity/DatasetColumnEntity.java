package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("dataset_column")
@Schema(name = "ModuleColumnEntity", description = "模板列实体")
public class DatasetColumnEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @TableField("column_name")
    @Schema(description = "列名")
    private String columnName;

    @TableField("column_type")
    @Schema(description = "列类型")
    private String columnType;

    @TableField("dataset_name")
    @Schema(description = "所属数据集名称")
    private String datasetName;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除")
    private Integer deleted;
}