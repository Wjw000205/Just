package org.example.just.dto.categoryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "ScienceCategoryTreeVO", description = "科学分类树节点")
public class ScienceCategoryTreeVO {

    @Schema(description = "分类ID", example = "1")
    private Integer id;

    @Schema(description = "分类名称", example = "生物医用材料（科学）")
    private String name;

    @Schema(description = "层级，从0开始", example = "0")
    private Integer level;

    @Schema(description = "数据集数量", example = "0")
    private Integer datasetCount;

    @Schema(description = "模板数量", example = "30")
    private Integer templateCount;

    @Schema(description = "子节点")
    private List<ScienceCategoryTreeVO> children = new ArrayList<>();
}
