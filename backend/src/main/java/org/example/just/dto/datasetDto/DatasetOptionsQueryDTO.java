package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "DatasetOptionsQueryDTO", description = "可用数据集查询参数")
public class DatasetOptionsQueryDTO {

    @Schema(description = "科学分类ID列表")
    private List<Integer> scienceCategoryIds;

    @Schema(description = "数据集名称关键字")
    private String keyword;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize;
}
