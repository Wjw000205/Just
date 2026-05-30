package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UpdateDatasetColumnDTO", description = "修改数据集字段请求参数")
public class UpdateDatasetColumnDTO {

    @Schema(description = "数据集字段ID", example = "22", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer columnId;

    @Schema(description = "字段名称", example = "烧结温度", requiredMode = Schema.RequiredMode.REQUIRED)
    private String columnName;

    @Schema(description = "字段类型", example = "double", requiredMode = Schema.RequiredMode.REQUIRED)
    private String columnType;
}
