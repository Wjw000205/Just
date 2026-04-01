package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "DatasetSearchRequest", description = "数据集检索请求参数")
public class DatasetSearchRequest {

    @Schema(description = "检索关键字")
    private String keyword;

    @Schema(description = "检索字段类型：title, fulltext, keyword_field, abstract, paragraph_keyword")
    private String searchField = "fulltext";

    @Schema(description = "选中的分类 ID 列表")
    private List<String> categoryIds = new ArrayList<>();

    @Schema(description = "检索范围：all, within_result")
    private String searchScope = "all";

    @Schema(description = "结果类型：dataset, paper, patent, standard, monograph, thesis")
    private String resultType;

    @Schema(description = "页码，从 1 开始，默认 1")
    private Integer page = 1;

    @Schema(description = "每页数量，默认 10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段：relevance, publishTime")
    private String sortField = "relevance";

    @Schema(description = "排序方式：asc, desc，默认 desc")
    private String sortOrder = "desc";

    @Schema(description = "高级检索条件")
    private AdvancedSearchCondition advanced;
}
