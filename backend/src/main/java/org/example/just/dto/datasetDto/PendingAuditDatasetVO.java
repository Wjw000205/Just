package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "PendingAuditDatasetVO", description = "待审核数据集返回对象")
public class PendingAuditDatasetVO {

    @Schema(description = "数据集ID")
    private Integer id;

    @Schema(description = "数据集名称")
    private String name;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "审核状态：0-待审核")
    private Integer auditStatus;

    @Schema(description = "数据级别：highvalue/public/private")
    private String dataLevel;

    @Schema(description = "数据集字段列表")
    private List<DatasetColumnAuditVO> columns = new ArrayList<>();
}
