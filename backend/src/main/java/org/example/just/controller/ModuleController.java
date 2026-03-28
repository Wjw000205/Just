package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.just.dto.moduleDto.CreateModuleDTO;
import org.example.just.dto.moduleDto.ModuleListVO;
import org.example.just.service.ModuleService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/module")
@Tag(name = "模板操作接口", description = "Module相关操作")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping("/create")
    @Operation(summary = "新建module", description = "向module表插入一条记录")
    public Result<String> createModule(@RequestBody CreateModuleDTO dto) {
        return moduleService.createModule(dto);
    }

    @GetMapping("/list-by-tag")
    @Operation(summary = "获取当前标签下所有模板", description = "根据tag查询该标签下所有未删除模板")
    public Result<List<ModuleListVO>> getModuleListByTag(@RequestParam("tag") String tag) {
        return moduleService.getModuleListByTag(tag);
    }
}