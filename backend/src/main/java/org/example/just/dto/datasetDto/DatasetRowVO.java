package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "ModuleRowVO", description = "模板单行数据")
public class DatasetRowVO {

    @Schema(description = "行ID")
    private Integer rowId;

    @Schema(description = "当前行数据，key为列名，value为单元格值")
    private Map<String, String> data;
}