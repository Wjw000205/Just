package org.example.just.dto.departmentDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UpdateDepartmentDTO", description = "修改部门请求参数")
public class UpdateDepartmentDTO {

    @Schema(description = "部门ID", example = "1")
    private Integer id;

    @Schema(description = "部门名称", example = "研发部")
    private String departmentName;

    @Schema(description = "部门负责人id", example = "1")
    private String leaderId;

    @Schema(description = "部门负责人姓名", example = "李四")
    private String leaderName;
}