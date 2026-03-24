package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.moduleDto.*;
import org.example.just.service.ModuleService;
import org.example.just.utils.PageQuery;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/module")
@Tag(name = "模板目录接口", description = "模板目录相关操作")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping("/create-menu")
    @Operation(summary = "创建目录", description = "仅向 module 表插入一条目录记录")
    public Result<String> createMenu(@RequestBody CreateModuleMenuDTO dto) {
        return moduleService.createMenu(dto);
    }

    @GetMapping("/getManuList")
    @Operation(summary = "查询模板目录树", description = "按父子关系返回模板目录树形结构")
    public Result<List<ModuleMenuVO>> getModuleTree() {
        return moduleService.getModuleTree();
    }

    @PostMapping("/create-template")
    @Operation(summary = "创建模板", description = "模板名称和父目录写入module表，模板列写入module_column表")
    public Result<String> createModule(@RequestBody CreateModuleDTO dto) {
        return moduleService.createModule(dto);
    }

    @PostMapping("/import-data")
    @Operation(summary = "导入模板数据", description = "上传Excel，将数据按模板列映射后批量插入module_data表")
    public Result<String> importModuleData(@RequestParam("moduleName") String moduleName,
                                           @RequestPart("file") MultipartFile file) {
        return moduleService.importModuleData(moduleName, file);
    }

    @GetMapping("/data-page")
    @Operation(summary = "按模板分页查询数据", description = "根据模板名称查询模板下所有数据，分页大小由前端指定")
    public Result<ModuleDataPageVO> getModuleDataPage(@RequestParam("moduleName") String moduleName,
                                                      PageQuery pageQuery) {
        return moduleService.getModuleDataPage(moduleName, pageQuery);
    }

    @GetMapping("/export-template")
    @Operation(summary = "导出模板结构", description = "只导出模板列为Excel表头，不导出具体数据")
    public void exportModuleTemplate(@RequestParam("moduleName") String moduleName,
                                     HttpServletResponse response) {
        moduleService.exportModuleTemplate(moduleName, response);
    }

    @PostMapping("/add-column")
    @Operation(summary = "新增模板列", description = "向指定模板新增一列，写入module_column表")
    public Result<String> addModuleColumn(@RequestBody AddModuleColumnDTO dto) {
        return moduleService.addModuleColumn(dto);
    }

    @DeleteMapping("/delete-column")
    @Operation(summary = "删除模板列", description = "逻辑删除模板列，并逻辑删除该列对应的模板数据")
    public Result<String> deleteModuleColumn(@RequestBody DeleteModuleColumnDTO dto) {
        return moduleService.deleteModuleColumn(dto);
    }

    @DeleteMapping("/delete-row")
    @Operation(summary = "删除模板具体行", description = "按模板名称和rowId逻辑删除该模板下的一整行数据")
    public Result<String> deleteModuleRow(@RequestBody DeleteModuleRowDTO dto) {
        return moduleService.deleteModuleRow(dto);
    }
    @PostMapping("/audit-template")
    @Operation(summary = "审核模板", description = "对指定模板进行状态变更，如通过或驳回")
    public Result<AuditModuleResultVO> auditModule(@RequestBody AuditModuleDTO dto) {
        return moduleService.auditModule(dto);
    }
}