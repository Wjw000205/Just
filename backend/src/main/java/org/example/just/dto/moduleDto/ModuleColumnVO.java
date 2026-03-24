package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ModuleColumnVO", description = "模板列返回对象")
public class ModuleColumnVO {

    @Schema(description = "列ID")
    private Integer id;

    @Schema(description = "列名")
    private String columnName;

    @Schema(description = "列类型")
    private String columnType;
}