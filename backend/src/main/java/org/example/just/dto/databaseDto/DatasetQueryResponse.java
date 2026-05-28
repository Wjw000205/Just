package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "DatasetQueryResponse", description = "数据集查询响应")
public class DatasetQueryResponse {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "数据集列表")
    private List<DatasetItem> list;

    @Data
    @Schema(name = "DatasetItem", description = "数据集项")
    public static class DatasetItem {
        
        @Schema(description = "数据集 ID")
        private Integer id;

        @Schema(description = "数据集名称")
        private String datasetName;

        @Schema(description = "学科分类列表")
        private List<String> scienceCategories;

        @Schema(description = "行业分类列表")
        private List<String> industryCategories;

        @Schema(description = "产品代码列表")
        private List<String> productCodes;

        @Schema(description = "数据级别：公益、内部等")
        private String dataLevel;

        @Schema(description = "数据条数")
        private Integer dataCount;

        @Schema(description = "模板名称")
        private String templateName;

        @Schema(description = "数据分类")
        private String dataCategory;

        @Schema(description = "所属部门")
        private String department;

        @Schema(description = "创建人")
        private String creator;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;

        @Schema(description = "更新时间")
        private LocalDateTime updateTime;

        @Schema(description = "是否已收藏")
        private Boolean favorited;
    }
}
