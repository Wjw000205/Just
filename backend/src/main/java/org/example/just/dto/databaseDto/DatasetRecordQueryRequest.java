package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DatasetRecordQueryRequest", description = "数据集记录查询请求")
public class DatasetRecordQueryRequest {

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页数量")
    private Integer pageSize = 20;
}
