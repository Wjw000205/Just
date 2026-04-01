package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "AdvancedSearchCondition", description = "高级检索条件")
public class AdvancedSearchCondition {

    @Schema(description = "多行条件，每项为 { field, value }")
    private List<ConditionItem> conditions = new ArrayList<>();

    @Schema(description = "发布日期起（含），格式 yyyy-MM-dd")
    private String publishDateStart;

    @Schema(description = "发布日期止（含），格式 yyyy-MM-dd")
    private String publishDateEnd;

    @Data
    @Schema(name = "ConditionItem", description = "单个条件项")
    public static class ConditionItem {
        @Schema(description = "字段名：title, keyword_field, abstract, publisher, organization")
        private String field;

        @Schema(description = "关键字值")
        private String value;
    }
}
