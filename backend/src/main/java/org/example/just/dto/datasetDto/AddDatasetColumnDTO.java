package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AddModuleColumnDTO", description = "新增模板列请求参数")
public class AddDatasetColumnDTO {

    @Schema(description = "模板名称", example = "人物细节模板")
    private String datasetName;

    @Schema(description = "列名称", example = "生命值")
    private String columnName;

    @Schema(description = "列类型", example = "int")
    private String columnType;
}