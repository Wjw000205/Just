package org.example.just.dto.categoryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ProductCategoryTreeResult", description = "产业/产品分类树响应")
public class ProductCategoryTreeResult {

    @Schema(description = "业务状态码，0表示成功", example = "0")
    private Integer code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "树形结构数组")
    private List<ProductCategoryTreeVO> data;

    @Schema(description = "根节点总数", example = "8")
    private Integer total;

    public static ProductCategoryTreeResult success(List<ProductCategoryTreeVO> data, Integer total) {
        ProductCategoryTreeResult result = new ProductCategoryTreeResult();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        result.setTotal(total);
        return result;
    }
}
