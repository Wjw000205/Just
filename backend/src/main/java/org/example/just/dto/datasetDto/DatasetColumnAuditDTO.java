package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DatasetColumnAuditDTO", description = "数据集字段审核请求")
public class DatasetColumnAuditDTO {

    @Schema(description = "数据集字段ID", example = "22", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer columnId;

    @Schema(description = "审核状态：1-审核通过，-1-审核不通过", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer state;
}
