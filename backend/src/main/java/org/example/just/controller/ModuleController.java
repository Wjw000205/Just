package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.just.dto.moduleDto.*;
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
    public Result<Integer> createModule(@RequestBody CreateModuleDTO dto) {
        return moduleService.createModule(dto);
    }

    @GetMapping("/list-by-tag")
    @Operation(summary = "获取当前标签下所有模板", description = "根据tag查询该标签下所有未删除模板")
    public Result<List<ModuleListVO>> getModuleListByTag(@RequestParam("tag") String tag) {
        return moduleService.getModuleListByTag(tag);
    }

    @GetMapping("/pending-audit-list")
    @Operation(summary = "获取待审核模板列表", description = "查询所有审核状态为0的模板")
    public Result<List<ModuleListVO>> getPendingAuditList() {
        return moduleService.getPendingAuditList();
    }

    @PostMapping("/audit")
    @Operation(summary = "审核模板", description = "更新模板审核状态：1-驳回，2-通过")
    public Result<String> auditModule(@RequestBody AuditModuleDTO dto) {
        return moduleService.auditModule(dto);
    }

    @GetMapping("/my-module")
    @Operation(summary = "获取我的模板", description = "根据创建者id查询当前用户创建的所有未删除模板")
    public Result<List<ModuleListVO>> getMyModuleList(@RequestParam("creatorId") Integer creatorId) {
        return moduleService.getMyModuleList(creatorId);
    }

    @PostMapping("/design")
    @Operation(summary = "模板设计", description = "根据模板id保存模板设计，object、operation、result写入module_column表")
    public Result<String> designModule(@RequestBody ModuleDesignDTO dto) {
        return moduleService.designModule(dto);
    }

    @GetMapping("/base-info")
    @Operation(summary = "获取模板基本信息", description = "根据模板ID获取模板基本信息")
    public Result<ModuleBaseInfoVO> getModuleBaseInfo(@RequestParam("id") Integer id) {
        return moduleService.getModuleBaseInfo(id);
    }

    @GetMapping("/detail-info")
    @Operation(summary = "获取模板具体字段信息", description = "根据模板ID获取模板具体字段信息，按Object、Operation、Result分别返回")
    public Result<ModuleDetailInfoVO> getModuleDetailInfo(@RequestParam("id") Integer id) {
        return moduleService.getModuleDetailInfo(id);
    }
}