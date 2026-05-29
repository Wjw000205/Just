package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AuditDatasetResultVO", description = "审核数据集返回对象")
public class AuditDatasetResultVO {

    @Schema(description = "数据集 ID")
    private Integer id;

    @Schema(description = "数据集名称")
    private String datasetName;

    @Schema(description = "审核状态：1-通过，2-驳回")
    private Integer status;

    @Schema(description = "审核意见")
    private String remark;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
}
