package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新建Module请求DTO")
public class CreateModuleDTO {

    @Schema(description = "模块名称", example = "用户管理模板")
    private String moduleName;

    @Schema(description = "模块标签", example = "user")
    private String tag;

    @Schema(description = "模块说明", example = "用于管理用户基础信息")
    private String description;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "可见范围 1:public 0:private", example = "1")
    private Integer visibleArea;

    @Schema(description = "是否同意/启用 1:yes 0:no", example = "1")
    private Integer agree;
}