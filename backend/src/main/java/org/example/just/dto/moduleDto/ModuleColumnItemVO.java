package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模板字段项")
public class ModuleColumnItemVO {

    @Schema(description = "列名")
    private String columnName;

    @Schema(description = "列类型")
    private String type;
}