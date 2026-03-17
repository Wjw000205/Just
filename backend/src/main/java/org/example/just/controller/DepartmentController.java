package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.just.dto.departmentDto.CreateDepartmentDTO;
import org.example.just.dto.departmentDto.UpdateDepartmentDTO;
import org.example.just.service.DepartmentService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/department")
@Tag(name = "部门接口", description = "部门相关操作")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping("/create")
    @Operation(summary = "新建部门", description = "创建新的部门")
    public Result<String> createDepartment(@RequestBody CreateDepartmentDTO dto) {
        return departmentService.createDepartment(dto);
    }

    @PutMapping("/update")
    @Operation(summary = "修改部门", description = "根据部门ID修改部门名称和负责人")
    public Result<String> updateDepartment(@RequestBody UpdateDepartmentDTO dto) {
        return departmentService.updateDepartment(dto);
    }
}