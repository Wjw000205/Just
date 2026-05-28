package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "DownloadRequest", description = "下载请求")
public class DownloadRequest {

    @Schema(description = "字段模式：all-全字段，partial-部分字段")
    private String fieldMode = "all";

    @Schema(description = "字段列表，fieldMode=partial 时必填")
    private List<String> fields;

    @Schema(description = "文件类型：json, xlsx")
    private String fileType = "json";

    @Schema(description = "数据结构：nested-嵌套，flat-平铺")
    private String dataShape = "nested";
}
