package org.example.just.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("dataset_data")
@Schema(name = "ModuleDataEntity", description = "模板数据实体")
public class DatasetDataEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("column_id")
    private Integer columnId;

    @TableField("row_id")
    private Integer rowId;

    @TableField("data_type")
    private String dataType;

    @TableField("data")
    private String data;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}