package org.example.just.dto.categoryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ScienceCategoryTreeQueryDTO", description = "科学分类树查询参数")
public class ScienceCategoryTreeQueryDTO {

    @Schema(description = "按名称模糊搜索", example = "生物医用")
    private String keyword;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "50")
    private Integer pageSize;
}
