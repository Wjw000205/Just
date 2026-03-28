package org.example.just.service;

import org.example.just.dto.moduleDto.CreateModuleDTO;
import org.example.just.dto.moduleDto.ModuleListVO;
import org.example.just.utils.Result;

import java.util.List;

public interface ModuleService {

    Result<String> createModule(CreateModuleDTO dto);

    Result<List<ModuleListVO>> getModuleListByTag(String tag);
}