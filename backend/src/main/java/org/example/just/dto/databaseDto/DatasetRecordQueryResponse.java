package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "DatasetRecordQueryResponse", description = "数据集记录查询响应")
public class DatasetRecordQueryResponse {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "记录列表")
    private List<RecordItem> list;

    @Data
    @Schema(name = "RecordItem", description = "记录项")
    public static class RecordItem {
        
        @Schema(description = "记录 ID")
        private String recordId;

        @Schema(description = "材料编号")
        private String materialCode;

        @Schema(description = "采集者")
        private String collector;

        @Schema(description = "采集者机构")
        private String collectorOrg;

        @Schema(description = "采集日期")
        private String collectDate;

        @Schema(description = "DOI")
        private String doi;

        @Schema(description = "实验者")
        private String experimenter;
    }
}
