package org.example.just.service;

import org.example.just.dto.departmentDto.CreateDepartmentDTO;
import org.example.just.dto.departmentDto.UpdateDepartmentDTO;
import org.example.just.utils.Result;

public interface DepartmentService {

    Result<String> createDepartment(CreateDepartmentDTO dto);

    Result<String> updateDepartment(UpdateDepartmentDTO dto);
}