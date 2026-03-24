package org.example.just.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.moduleDto.*;
import org.example.just.utils.PageQuery;
import org.example.just.utils.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ModuleService {

    Result<String> createMenu(CreateModuleMenuDTO dto);

    Result<List<ModuleMenuVO>> getModuleTree();

    Result<String> createModule(CreateModuleDTO dto);

    Result<String> importModuleData(String moduleName, MultipartFile file);

    Result<ModuleDataPageVO> getModuleDataPage(String moduleName, PageQuery pageQuery);

    void exportModuleTemplate(String moduleName, HttpServletResponse response);

    Result<String> addModuleColumn(AddModuleColumnDTO dto);

    Result<String> deleteModuleColumn(DeleteModuleColumnDTO dto);

    Result<String> deleteModuleRow(DeleteModuleRowDTO dto);
}