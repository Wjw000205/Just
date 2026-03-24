package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CreateModuleMenuDTO", description = "创建模板目录请求参数")
public class CreateModuleMenuDTO {

    @Schema(description = "目录名称", example = "人物模板")
    private String moduleName;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "父级目录名称；一级目录可不传，二级目录必须传", example = "基础模板")
    private String parent;
}