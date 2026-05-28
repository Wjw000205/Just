package org.example.just.dto.categoryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "ProductCategoryTreeVO", description = "产业/产品分类树节点")
public class ProductCategoryTreeVO {

    @Schema(description = "分类ID", example = "p1")
    private String id;

    @Schema(description = "产业代码", example = "3")
    private String industryCode;

    @Schema(description = "产业分类名称", example = "新材料产业")
    private String industryName;

    @Schema(description = "行业代码")
    private String sectorCode;

    @Schema(description = "行业分类名称")
    private String sectorName;

    @Schema(description = "产品代码")
    private String productCode;

    @Schema(description = "产品分类名称")
    private String productName;

    @Schema(description = "子节点")
    private List<ProductCategoryTreeVO> children = new ArrayList<>();
}
