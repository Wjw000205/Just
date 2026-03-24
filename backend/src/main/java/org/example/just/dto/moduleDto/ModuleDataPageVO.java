package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.just.utils.PageResult;

import java.util.List;

@Data
@Schema(name = "ModuleDataPageVO", description = "模板数据分页返回对象")
public class ModuleDataPageVO {

    @Schema(description = "模板名称")
    private String moduleName;

    @Schema(description = "模板列定义")
    private List<ModuleColumnVO> columns;

    @Schema(description = "分页行数据")
    private PageResult<ModuleRowVO> pageData;
}