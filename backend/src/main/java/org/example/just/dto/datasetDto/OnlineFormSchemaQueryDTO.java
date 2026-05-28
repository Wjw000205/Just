package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OnlineFormSchemaQueryDTO", description = "在线填写表单结构查询参数")
public class OnlineFormSchemaQueryDTO {

    @Schema(description = "数据集ID", example = "123")
    private Integer datasetId;
}
