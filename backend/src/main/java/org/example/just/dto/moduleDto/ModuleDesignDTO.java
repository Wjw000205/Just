package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "模板设计请求参数")
public class ModuleDesignDTO {

    @Schema(description = "模板id", example = "1")
    private Integer moduleId;

    @Schema(description = "Object字段列表")
    private List<ModuleColumnItemDTO> object;

    @Schema(description = "Operation字段列表")
    private List<ModuleColumnItemDTO> operation;

    @Schema(description = "Result字段列表")
    private List<ModuleColumnItemDTO> result;
}