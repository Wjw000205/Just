package org.example.just.service;

import org.example.just.dto.moduleDto.*;
import org.example.just.utils.Result;

import java.util.List;

public interface ModuleService {

    Result<Integer> createModule(CreateModuleDTO dto);

    Result<List<ModuleListVO>> getModuleListByTag(String tag);

    Result<List<ModuleListVO>> getPendingAuditList();

    Result<String> auditModule(AuditModuleDTO dto);

    Result<List<ModuleListVO>> getMyModuleList(Integer creatorId);

    Result<String> designModule(ModuleDesignDTO dto);

    Result<ModuleBaseInfoVO> getModuleBaseInfo(Integer id);

    Result<ModuleDetailInfoVO> getModuleDetailInfo(Integer id);
}