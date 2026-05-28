package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "CreateDatasetResultVO", description = "创建数据集返回数据")
public class CreateDatasetResultVO {

    @Schema(description = "新建数据集ID", example = "123")
    private Integer datasetId;

    @Schema(description = "数据集名称", example = "羟基磷灰石粉末性能数据集")
    private String name;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
