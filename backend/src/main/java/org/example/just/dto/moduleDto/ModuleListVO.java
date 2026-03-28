package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "模板列表返回对象")
public class ModuleListVO {

    @Schema(description = "主键id")
    private Integer id;

    @Schema(description = "模板名称")
    private String moduleName;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "模板说明")
    private String description;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "可见范围 1:public 0:private")
    private Integer visibleArea;

    @Schema(description = "是否同意/启用 1:yes 0:no")
    private Integer agree;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}