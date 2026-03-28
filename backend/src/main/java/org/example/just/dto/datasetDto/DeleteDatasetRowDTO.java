package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DeleteModuleRowDTO", description = "删除模板具体行请求参数")
public class DeleteDatasetRowDTO {

    @Schema(description = "模板名称", example = "人物细节模板")
    private String datasetName;

    @Schema(description = "行ID", example = "1")
    private Integer rowId;
}