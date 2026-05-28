package org.example.just.dto.categoryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ProductCategoryTreeQueryDTO", description = "产业/产品分类树查询参数")
public class ProductCategoryTreeQueryDTO {

    @Schema(description = "产业分类搜索关键字", example = "新材料")
    private String industryKeyword;

    @Schema(description = "行业分类搜索关键字")
    private String sectorKeyword;

    @Schema(description = "产品分类搜索关键字")
    private String productKeyword;
}
