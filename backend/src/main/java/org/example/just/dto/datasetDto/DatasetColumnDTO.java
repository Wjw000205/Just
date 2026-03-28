package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ModuleColumnDTO", description = "模板列定义")
public class DatasetColumnDTO {

    @Schema(description = "列名", example = "角色名称")
    private String columnName;

    @Schema(description = "列类型", example = "varchar")
    private String columnType;
}