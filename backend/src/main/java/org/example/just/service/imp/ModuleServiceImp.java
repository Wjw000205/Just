package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.just.dao.ModuleColumnDao;
import org.example.just.dao.UserDao;
import org.example.just.dto.moduleDto.*;
import org.example.just.entity.ModuleColumnEntity;
import org.example.just.entity.UserEntity;
import org.springframework.transaction.annotation.Transactional;
import org.example.just.dao.ModuleDao;
import org.example.just.entity.ModuleEntity;
import org.example.just.service.ModuleService;
import org.example.just.utils.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ModuleServiceImp implements ModuleService {

    private final ModuleDao moduleDao;
    private final ModuleColumnDao moduleColumnDao;
    private final UserDao userDao;

    public ModuleServiceImp(ModuleDao moduleDao,
                            ModuleColumnDao moduleColumnDao,
                            UserDao userDao) {
        this.moduleDao = moduleDao;
        this.moduleColumnDao = moduleColumnDao;
        this.userDao = userDao;
    }

    @Override
    @Transactional
    public Result<Integer> createModule(CreateModuleDTO dto) {

        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }

        if (dto.getModuleName() == null || dto.getModuleName().trim().isEmpty()) {
            return Result.fail("moduleName不能为空");
        }

        if (dto.getTag() == null || dto.getTag().trim().isEmpty()) {
            return Result.fail("tag不能为空");
        }

        if (dto.getCreator() == null) {
            return Result.fail("creator不能为空");
        }

        if (dto.getVisibleArea() == null) {
            return Result.fail("visibleArea不能为空");
        }

        if (dto.getAgree() == null) {
            return Result.fail("agree不能为空");
        }

        String moduleName = dto.getModuleName().trim();
        String tag = dto.getTag().trim();

        // 同一标签下不能存在相同模板
        LambdaQueryWrapper<ModuleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleEntity::getTag, tag)
                .eq(ModuleEntity::getModuleName, moduleName)
                .eq(ModuleEntity::getDeleted, 0);

        Long count = moduleDao.selectCount(queryWrapper);
        if (count != null && count > 0) {
            return Result.fail("该标签下已存在同名模板");
        }

        ModuleEntity entity = new ModuleEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setModuleName(moduleName);
        entity.setTag(tag);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());

        int rows = moduleDao.insert(entity);

        if (rows > 0 && entity.getId() != null) {
            return Result.success(entity.getId());
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
    public Result<List<TemplateTagVO>> getTemplateTags() {
        List<ModuleEntity> modules = listAvailableModules();
        if (modules.isEmpty()) {
            return Result.success(0, "success", new ArrayList<>());
        }

        Map<String, TemplateTagVO> tagMap = new LinkedHashMap<>();
        for (ModuleEntity module : modules) {
            if (module == null || !StringUtils.hasText(module.getTag())) {
                continue;
            }
            String tagName = module.getTag().trim();
            TemplateTagVO existing = tagMap.get(tagName);
            if (existing == null) {
                tagMap.put(tagName, new TemplateTagVO(module.getId(), tagName));
            } else if (module.getId() != null
                    && (existing.getId() == null || module.getId() < existing.getId())) {
                existing.setId(module.getId());
            }
        }

        return Result.success(0, "success", new ArrayList<>(tagMap.values()));
    }

    @Override
    public Result<List<TemplateOptionVO>> getTemplateOptions(Integer templateTagId, Integer scienceCategoryId) {
        List<ModuleEntity> modules = listAvailableModules(scienceCategoryId);
        if (modules.isEmpty()) {
            return Result.success(0, "success", new ArrayList<>());
        }

        String tagName = null;
        if (templateTagId != null) {
            tagName = resolveTemplateTagName(modules, templateTagId);
            if (!StringUtils.hasText(tagName)) {
                return Result.success(0, "success", new ArrayList<>());
            }
        }

        Map<Integer, TemplateOptionVO> optionMap = new LinkedHashMap<>();
        for (ModuleEntity module : modules) {
            if (module == null || module.getId() == null || !StringUtils.hasText(module.getModuleName())) {
                continue;
            }
            if (StringUtils.hasText(tagName)
                    && (!StringUtils.hasText(module.getTag()) || !tagName.equals(module.getTag().trim()))) {
                continue;
            }
            optionMap.putIfAbsent(module.getId(), new TemplateOptionVO(module.getId(), module.getModuleName().trim()));
        }

        return Result.success(0, "success", new ArrayList<>(optionMap.values()));
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

    @Override
    @Transactional
    public Result<String> designModule(ModuleDesignDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getModuleId() == null) {
            return Result.fail("模板id不能为空");
        }

        boolean objectEmpty = CollectionUtils.isEmpty(dto.getObject());
        boolean operationEmpty = CollectionUtils.isEmpty(dto.getOperation());
        boolean resultEmpty = CollectionUtils.isEmpty(dto.getResult());

        if (objectEmpty && operationEmpty && resultEmpty) {
            return Result.fail("模板设计内容不能为空");
        }

        // 1. 校验模板是否存在
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getId, dto.getModuleId())
                .eq(ModuleEntity::getDeleted, 0)
                .last("limit 1");

        ModuleEntity moduleEntity = moduleDao.selectOne(moduleWrapper);
        if (moduleEntity == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验三组字段
        String validateMsg = validateColumnList(dto.getObject(), "Object");
        if (validateMsg != null) {
            return Result.fail(validateMsg);
        }

        validateMsg = validateColumnList(dto.getOperation(), "Operation");
        if (validateMsg != null) {
            return Result.fail(validateMsg);
        }

        validateMsg = validateColumnList(dto.getResult(), "Result");
        if (validateMsg != null) {
            return Result.fail(validateMsg);
        }

        // 3. 先逻辑删除旧设计
        LambdaUpdateWrapper<ModuleColumnEntity> deleteWrapper = new LambdaUpdateWrapper<>();
        deleteWrapper.eq(ModuleColumnEntity::getModuleId, dto.getModuleId())
                .eq(ModuleColumnEntity::getDeleted, 0)
                .set(ModuleColumnEntity::getDeleted, 1);

        moduleColumnDao.update(null, deleteWrapper);

        // 4. 插入新设计
        saveModuleColumns(dto.getModuleId(), dto.getObject(), "Object");
        saveModuleColumns(dto.getModuleId(), dto.getOperation(), "Operation");
        saveModuleColumns(dto.getModuleId(), dto.getResult(), "Result");

        return Result.success("模板设计保存成功");
    }

    @Override
    public Result<ModuleBaseInfoVO> getModuleBaseInfo(Integer id) {
        if (id == null) {
            return Result.fail("模板id不能为空");
        }

        LambdaQueryWrapper<ModuleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleEntity::getId, id)
                .eq(ModuleEntity::getDeleted, 0)
                .last("limit 1");

        ModuleEntity moduleEntity = moduleDao.selectOne(queryWrapper);
        if (moduleEntity == null) {
            return Result.fail("模板不存在");
        }

        ModuleBaseInfoVO vo = new ModuleBaseInfoVO();
        BeanUtils.copyProperties(moduleEntity, vo);
        if (moduleEntity.getCreator() != null) {
            UserEntity creator = userDao.selectById(moduleEntity.getCreator());
            if (creator != null) {
                vo.setUsername(creator.getUsername());
            }
        }

        return Result.success(vo);
    }

    @Override
    public Result<ModuleDetailInfoVO> getModuleDetailInfo(Integer id) {
        if (id == null) {
            return Result.fail("模板id不能为空");
        }

        // 1. 校验模板是否存在
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getId, id)
                .eq(ModuleEntity::getDeleted, 0)
                .last("limit 1");

        ModuleEntity moduleEntity = moduleDao.selectOne(moduleWrapper);
        if (moduleEntity == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查询模板字段
        LambdaQueryWrapper<ModuleColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(ModuleColumnEntity::getModuleId, id)
                .eq(ModuleColumnEntity::getDeleted, 0)
                .orderByAsc(ModuleColumnEntity::getCreateTime)
                .orderByAsc(ModuleColumnEntity::getId);

        List<ModuleColumnEntity> columnEntities = moduleColumnDao.selectList(columnWrapper);

        ModuleDetailInfoVO vo = new ModuleDetailInfoVO();
        vo.setModuleId(id);

        for (ModuleColumnEntity entity : columnEntities) {
            ModuleColumnItemVO itemVO = new ModuleColumnItemVO();
            itemVO.setColumnName(entity.getColumnName());
            itemVO.setType(entity.getType());

            String belong = entity.getBelong();
            if ("Object".equalsIgnoreCase(belong)) {
                vo.getObject().add(itemVO);
            } else if ("Operation".equalsIgnoreCase(belong)) {
                vo.getOperation().add(itemVO);
            } else if ("Result".equalsIgnoreCase(belong)) {
                vo.getResult().add(itemVO);
            }
        }

        return Result.success(vo);
    }

    private List<ModuleEntity> listAvailableModules() {
        return listAvailableModules(null);
    }

    private List<ModuleEntity> listAvailableModules(Integer scienceCategoryId) {
        List<ModuleEntity> modules = scienceCategoryId == null
                ? moduleDao.selectAvailableTemplates()
                : moduleDao.selectAvailableTemplatesByScienceCategory(scienceCategoryId);
        if (modules == null || modules.isEmpty()) {
            return new ArrayList<>();
        }
        return modules.stream()
                .filter(Objects::nonNull)
                .filter(module -> module.getDeleted() == null || module.getDeleted() == 0)
                .filter(module -> module.getAuditState() == null || module.getAuditState() == 2)
                .sorted((left, right) -> {
                    Integer leftId = left.getId() == null ? Integer.MAX_VALUE : left.getId();
                    Integer rightId = right.getId() == null ? Integer.MAX_VALUE : right.getId();
                    return leftId.compareTo(rightId);
                })
                .collect(Collectors.toList());
    }

    private String resolveTemplateTagName(List<ModuleEntity> modules, Integer templateTagId) {
        if (templateTagId == null || CollectionUtils.isEmpty(modules)) {
            return null;
        }

        Map<String, Integer> tagIdMap = new LinkedHashMap<>();
        for (ModuleEntity module : modules) {
            if (module == null || module.getId() == null || !StringUtils.hasText(module.getTag())) {
                continue;
            }
            String tagName = module.getTag().trim();
            Integer existingId = tagIdMap.get(tagName);
            if (existingId == null || module.getId() < existingId) {
                tagIdMap.put(tagName, module.getId());
            }
        }

        for (Map.Entry<String, Integer> entry : tagIdMap.entrySet()) {
            if (templateTagId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String validateColumnList(List<ModuleColumnItemDTO> list, String belong) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        Set<String> columnNameSet = new HashSet<>();
        for (ModuleColumnItemDTO item : list) {
            if (item == null) {
                return belong + "字段项不能为空";
            }
            if (!StringUtils.hasText(item.getColumnName())) {
                return belong + "中存在空列名";
            }
            if (!StringUtils.hasText(item.getType())) {
                return belong + "中存在空列类型";
            }

            String columnName = item.getColumnName().trim();
            if (!columnNameSet.add(columnName)) {
                return belong + "中存在重复列名: " + columnName;
            }
        }
        return null;
    }

    private void saveModuleColumns(Integer moduleId, List<ModuleColumnItemDTO> list, String belong) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        for (ModuleColumnItemDTO item : list) {
            ModuleColumnEntity entity = new ModuleColumnEntity();
            entity.setModuleId(moduleId);
            entity.setColumnName(item.getColumnName().trim());
            entity.setType(item.getType().trim());
            entity.setBelong(belong);
            entity.setCreateTime(LocalDateTime.now());
            entity.setDeleted(0);

            int rows = moduleColumnDao.insert(entity);
            if (rows <= 0) {
                throw new RuntimeException("保存模板设计失败");
            }
        }
    }
}
