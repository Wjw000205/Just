package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "CreateDatasetDTO", description = "创建数据集请求参数")
public class CreateDatasetDTO {

    @Schema(description = "数据集名称，0-200字", example = "羟基磷灰石粉末性能数据集")
    private String name;

    @Schema(description = "数据集摘要，0-500字", example = "收集了不同烧结温度下羟基磷灰石粉末的粒径分布、比表面积等数据。")
    private String summary;

    @Schema(description = "封面图URL", example = "https://xxx.com/cover/abc.png")
    private String coverUrl;

    @Schema(description = "科学分类树中选中的节点ID", example = "1001")
    private Integer scienceCategoryId;

    @Schema(description = "产业/产品分类表格中选中的节点ID", example = "3001")
    private Integer productCategoryId;

    @Schema(description = "数据级别：highvalue/public/private", example = "public")
    private String dataLevel;

    @Schema(description = "数据类别，当前固定为dataset", example = "dataset")
    private String dataCategory;

    @Schema(description = "模板标签ID", example = "11")
    private Integer templateTagId;

    @Schema(description = "模板ID", example = "5")
    private Integer templateId;

    @Schema(description = "数据集标签ID列表")
    private List<Integer> datasetTagIds;
}
