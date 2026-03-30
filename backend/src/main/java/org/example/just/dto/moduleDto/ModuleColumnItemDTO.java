package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模板设计字段项")
public class ModuleColumnItemDTO {

    @Schema(description = "列名", example = "name")
    private String columnName;

    @Schema(description = "列类型", example = "String")
    private String type;
}