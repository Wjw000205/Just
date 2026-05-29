package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DatasetOptionsVO", description = "可选数据集项")
public class DatasetOptionsVO {

    @Schema(description = "数据集ID", example = "123")
    private Integer id;

    @Schema(description = "数据集名称")
    private String name;

    @Schema(description = "数据级别", example = "public")
    private String dataLevel;

    @Schema(description = "审核状态：0未审核，1审核通过，2审核未通过")
    private Integer auditStatus;

    @Schema(description = "已上传记录数", example = "256")
    private Long recordCount;
}
