package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AuditDatasetDTO", description = "审核数据集请求参数")
public class AuditDatasetDTO {

    @Schema(description = "数据集名称", example = "羟基磷灰石粉末性能数据集", requiredMode = Schema.RequiredMode.REQUIRED)
    private String datasetName;

    @Schema(description = "审核状态：0-驳回，1-通过", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "审核意见", example = "审核通过")
    private String remark;

    @Schema(description = "审核人", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String auditor;
}
