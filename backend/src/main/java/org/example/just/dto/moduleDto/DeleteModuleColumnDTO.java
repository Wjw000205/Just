package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "DeleteModuleColumnDTO", description = "删除模板列请求参数")
public class DeleteModuleColumnDTO {

    @Schema(description = "模板列ID", example = "1")
    private Integer columnId;
}