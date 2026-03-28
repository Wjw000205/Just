package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.just.dto.moduleDto.AuditModuleDTO;
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
                .eq(ModuleEntity::getAuditState, 2)
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

    @Override
    public Result<List<ModuleListVO>> getPendingAuditList() {
        LambdaQueryWrapper<ModuleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleEntity::getDeleted, 0)
                .eq(ModuleEntity::getAuditState, 0)
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

    @Override
    @Transactional
    public Result<String> auditModule(AuditModuleDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getId() == null) {
            return Result.fail("模板id不能为空");
        }
        if (dto.getAuditState() == null) {
            return Result.fail("审核状态不能为空");
        }
        if (dto.getAuditState() != 1 && dto.getAuditState() != 2) {
            return Result.fail("审核状态非法，只能为1(驳回)或2(通过)");
        }

        ModuleEntity moduleEntity = moduleDao.selectById(dto.getId());
        if (moduleEntity == null || (moduleEntity.getDeleted() != null && moduleEntity.getDeleted() == 1)) {
            return Result.fail("模板不存在");
        }

        if (moduleEntity.getAuditState() != null && moduleEntity.getAuditState() != 0) {
            return Result.fail("该模板已审核，不能重复审核");
        }

        ModuleEntity updateEntity = new ModuleEntity();
        updateEntity.setId(dto.getId());
        updateEntity.setAuditState(dto.getAuditState());

        int rows = moduleDao.updateById(updateEntity);
        if (rows <= 0) {
            return Result.fail("审核操作失败");
        }

        if (dto.getAuditState() == 1) {
            return Result.success("驳回成功");
        }
        return Result.success("审核通过成功");
    }

    @Override
    public Result<List<ModuleListVO>> getMyModuleList(Integer creatorId) {
        if (creatorId == null) {
            return Result.fail("creatorId不能为空");
        }

        LambdaQueryWrapper<ModuleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleEntity::getCreator, creatorId)
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