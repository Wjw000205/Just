package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "DownloadFieldVO", description = "可下载字段响应")
public class DownloadFieldVO {

    @Schema(description = "字段列表")
    private List<FieldItem> fields;

    @Data
    @Schema(name = "FieldItem", description = "字段项")
    public static class FieldItem {
        
        @Schema(description = "字段标签名")
        private String label;

        @Schema(description = "字段值（key）")
        private String value;
    }
}
