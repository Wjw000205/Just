package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DatasetQueryRequest", description = "数据集查询请求参数")
public class DatasetQueryRequest {

    @Schema(description = "数据集名称")
    private String datasetName;

    @Schema(description = "行业分类")
    private String industryCategory;

    @Schema(description = "数据分类")
    private String dataCategory;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "学科分类范围：all, within_result")
    private String scienceCategoryScope = "all";

    @Schema(description = "分类关键字")
    private String categoryKeyword;

    @Schema(description = "页码，从 1 开始，默认 1")
    private Integer page = 1;

    @Schema(description = "每页数量，默认 10")
    private Integer pageSize = 10;
}
