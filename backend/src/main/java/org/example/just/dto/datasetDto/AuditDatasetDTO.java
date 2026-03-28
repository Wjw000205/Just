package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AuditModuleDTO", description = "审核模板请求参数")
public class AuditDatasetDTO {

    @Schema(description = "模板名称", example = "人物模板")
    private String datasetName;

    @Schema(description = "审核状态：0-驳回，1-通过", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "审核意见", example = "审核通过")
    private String remark;

    @Schema(description = "审核人", example = "admin")
    private String auditor;
}
