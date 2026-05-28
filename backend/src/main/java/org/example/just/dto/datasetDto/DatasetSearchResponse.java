package org.example.just.dto.datasetDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "DatasetSearchResponse", description = "数据集检索响应")
public class DatasetSearchResponse {

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "每页数量")
    @JsonProperty("pageSize")
    private Integer pageSize;

    @Schema(description = "总记录数")
    private Integer total;

    @Schema(description = "数据列表")
    private List<DatasetItem> items;

    @Data
    @Schema(name = "DatasetItem", description = "数据集项")
    public static class DatasetItem {
        
        @Schema(description = "数据集唯一标识")
        private String id;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "摘要/简介")
        private String abstractText;

        @Schema(description = "分类路径")
        private List<CategoryPathItem> categoryPath;

        @Schema(description = "关键字标签数组")
        private List<String> tags;

        @Schema(description = "发布时间")
        private String publishTime;

        @Schema(description = "创建人/第一作者")
        private String creator;

        @Schema(description = "所属机构")
        private String organization;

        @Schema(description = "下载次数")
        private Integer downloadCount;

        @Schema(description = "浏览次数")
        private Integer viewCount;

        @Schema(description = "是否有全文可查看/下载")
        private Boolean hasFullText;

        @Schema(description = "预留扩展信息字典")
        private Object extra;

        @Data
        @Schema(name = "CategoryPathItem", description = "分类路径项")
        public static class CategoryPathItem {
            
            @Schema(description = "分类 id")
            private String id;

            @Schema(description = "分类名称")
            private String name;
        }
    }
}
