package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "ModuleTreeVO", description = "模板目录树形结构返回对象")
public class ModuleMenuVO {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "目录名称")
    private String moduleName;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "父级目录")
    private String parent;

    @Schema(description = "是否目录：1是目录，0不是目录")
    private Integer isMenu;

    @Schema(description = "子目录")
    private List<ModuleMenuVO> children = new ArrayList<>();
}