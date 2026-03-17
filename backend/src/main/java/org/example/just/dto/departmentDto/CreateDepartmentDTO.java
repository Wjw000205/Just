package org.example.just.dto.departmentDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CreateDepartmentDTO", description = "新建部门请求参数")
public class CreateDepartmentDTO {

    @Schema(description = "部门名称", example = "技术部")
    private String departmentName;

    @Schema(description = "部门负责人", example = "张三")
    private String leader;
}