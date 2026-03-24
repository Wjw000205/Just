package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "CreateModuleDTO", description = "创建模板请求参数")
public class CreateModuleDTO {

    @Schema(description = "模板名称", example = "人物模板")
    private String moduleName;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "父目录名称", example = "基础模板")
    private String parent;

    @Schema(description = "模板列集合")
    private List<ModuleColumnDTO> columns;
}