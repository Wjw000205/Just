package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.just.dto.moduleDto.ModuleListVO;
import org.springframework.transaction.annotation.Transactional;
import org.example.just.dao.ModuleDao;
import org.example.just.dto.moduleDto.CreateModuleDTO;
import org.example.just.entity.ModuleEntity;
import org.example.just.service.ModuleService;
import org.example.just.utils.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ModuleServiceImp implements ModuleService {

    private final ModuleDao moduleDao;

    public ModuleServiceImp(ModuleDao moduleDao) {
        this.moduleDao = moduleDao;
    }

    @Override
    @Transactional
    public Result<String> createModule(CreateModuleDTO dto) {

        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }

        if (dto.getModuleName() == null || dto.getModuleName().trim().isEmpty()) {
            return Result.fail("moduleName不能为空");
        }

        if (dto.getTag() == null || dto.getTag().trim().isEmpty()) {
            return Result.fail("tag不能为空");
        }

        if (dto.getCreator() == null || dto.getCreator().trim().isEmpty()) {
            return Result.fail("creator不能为空");
        }

        if (dto.getVisibleArea() == null) {
            return Result.fail("visibleArea不能为空");
        }

        if (dto.getAgree() == null) {
            return Result.fail("agree不能为空");
        }

        // 同一标签下不能存在相同模板
        LambdaQueryWrapper<ModuleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleEntity::getTag, dto.getTag())
                .eq(ModuleEntity::getModuleName, dto.getModuleName())
                .eq(ModuleEntity::getDeleted, 0);

        Long count = moduleDao.selectCount(queryWrapper);
        if (count != null && count > 0) {
            return Result.fail("该标签下已存在同名模板");
        }

        ModuleEntity entity = new ModuleEntity();
        BeanUtils.copyProperties(dto, entity);

        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());

        int rows = moduleDao.insert(entity);

        if (rows > 0) {
            return Result.success("新建module成功");
        }

        return Result.fail("新建module失败");
    }

    @Override
    public Result<List<ModuleListVO>> getModuleListByTag(String tag) {
        if (!StringUtils.hasText(tag)) {
            return Result.fail("tag不能为空");
        }

        LambdaQueryWrapper<ModuleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleEntity::getTag, tag.trim())
                .eq(ModuleEntity::getDeleted, 0)
                .orderByDesc(ModuleEntity::getCreateTime);

        List<ModuleEntity> moduleEntities = moduleDao.selectList(queryWrapper);

        List<ModuleListVO> resultList = new ArrayList<>();
        for (ModuleEntity entity : moduleEntities) {
            ModuleListVO vo = new ModuleListVO();
            BeanUtils.copyProperties(entity, vo);
            resultList.add(vo);
        }

        return Result.success(resultList);
    }
}