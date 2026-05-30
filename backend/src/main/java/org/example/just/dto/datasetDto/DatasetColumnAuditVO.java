package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DatasetColumnAuditVO", description = "数据集字段审核项")
public class DatasetColumnAuditVO {

    @Schema(description = "数据集字段ID", example = "22")
    private Integer id;

    @Schema(description = "数据集名称", example = "羟基磷灰石粉末性能数据集")
    private String datasetName;

    @Schema(description = "字段名称", example = "烧结温度")
    private String columnName;

    @Schema(description = "字段类型", example = "varchar")
    private String columnType;

    @Schema(description = "审核状态：1-审核通过，0-待审核，-1-审核不通过", example = "0")
    private Integer state;
}
