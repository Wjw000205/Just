package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "CategoryTreeNode", description = "分类树节点")
public class CategoryTreeNode {

    @Schema(description = "分类唯一标识")
    private String id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "子节点数组")
    private List<CategoryTreeNode> children;
}
