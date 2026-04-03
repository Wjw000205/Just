package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.example.just.context.UserContext;
import org.example.just.dao.*;
import org.example.just.dto.databaseDto.*;
import org.example.just.entity.*;
import org.example.just.service.DataBaseService;
import org.example.just.service.DatasetService;
import org.example.just.service.ModuleService;
import org.example.just.utils.JwtUtil;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DataBaseServiceImpl implements DataBaseService {

    @Resource
    private IndustryClassificationDao industryClassificationDao;
    
    @Resource
    private ManuDatasetDao manuDatasetDao;
    
    @Resource
    private ModuleDao moduleDao;
    
    @Resource
    private UserDepartmentDao userDepartmentDao;
    
    @Resource
    private DepartmentDao departmentDao;
    
    @Resource
    private DatasetColumnDao datasetColumnDao;
    
    @Resource
    private DatasetDataDao datasetDataDao;

    @Resource
    private DatasetService datasetService;

    @Resource
    private ModuleService moduleService;

    @Resource
    private FavoriteDao favoriteDao;


    @Resource
    private DatabasePageStaticDataDao databasePageStaticDataDao;


    @Override
    public Result<DataBasePageInitInfoVO> getPageInitInfo() {
        DataBasePageInitInfoVO pageInitInfoVO = new DataBasePageInitInfoVO();

        // 设置左侧目录树
        pageInitInfoVO.setTree(getClassificationTree());

        // 设置行业分类下拉选项（需要从数据库或配置获取）
        pageInitInfoVO.setIndustryCategories(getIndustryCategories());

        // 设置数据分类下拉选项（固定值或从配置获取）
        pageInitInfoVO.setDataCategories(getDataCategories());

        // 设置部门下拉选项（从数据库获取）
        pageInitInfoVO.setDepartments(getDepartments());



        pageInitInfoVO.setTotal(datasetService.getTotal());
        return Result.success(pageInitInfoVO);
    }

        /**
     * 获取行业分类下拉选项
     */
    private List<DataBasePageInitInfoVO.OptionItem> getIndustryCategories() {
        List<DataBasePageInitInfoVO.OptionItem> options = new ArrayList<>();

        // 从 industry_classification 表查询顶级行业分类（parentId 为 null 或空）
        LambdaQueryWrapper<IndustryClassificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(IndustryClassificationEntity::getParentId)
               .or()
               .eq(IndustryClassificationEntity::getParentId, "")
               .orderByAsc(IndustryClassificationEntity::getSortOrder);

        List<IndustryClassificationEntity> industries = industryClassificationDao.selectList(wrapper);

        for (IndustryClassificationEntity industry : industries) {
            options.add(createOption(industry.getName(), "industry_" + industry.getId()));
        }

        return options;
    }

    /**
     * 获取数据分类下拉选项
     */
    private List<DataBasePageInitInfoVO.OptionItem> getDataCategories() {
        List<DataBasePageInitInfoVO.OptionItem> options = new ArrayList<>();

        // 从数据库查询数据分类
        LambdaQueryWrapper<DatabasePageStaticDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DatabasePageStaticDataEntity::getCategoryType, "data_category")
               .eq(DatabasePageStaticDataEntity::getParentId, 0)
               .orderByAsc(DatabasePageStaticDataEntity::getSortOrder);

        List<DatabasePageStaticDataEntity> dataCategories = databasePageStaticDataDao.selectList(wrapper);

        for (DatabasePageStaticDataEntity item : dataCategories) {
            options.add(createOption(item.getName(), item.getCategoryType() + "_" + item.getId()));
        }

        return options;
    }

    /**
     * 获取部门下拉选项
     */
    private List<DataBasePageInitInfoVO.OptionItem> getDepartments() {
        List<DataBasePageInitInfoVO.OptionItem> options = new ArrayList<>();

        // 从数据库查询部门列表
        LambdaQueryWrapper<DatabasePageStaticDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DatabasePageStaticDataEntity::getCategoryType, "department")
               .eq(DatabasePageStaticDataEntity::getParentId, 0)
               .orderByAsc(DatabasePageStaticDataEntity::getSortOrder);

        List<DatabasePageStaticDataEntity> departments = databasePageStaticDataDao.selectList(wrapper);

        for (DatabasePageStaticDataEntity dept : departments) {
            options.add(createOption(dept.getName(), "dept_" + dept.getId()));
        }

        return options;
    }


    /**
     * 辅助方法：创建选项
     */
    private DataBasePageInitInfoVO.OptionItem createOption(String label, String value) {
        DataBasePageInitInfoVO.OptionItem option = new DataBasePageInitInfoVO.OptionItem();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }


    private List<DataBasePageInitInfoVO.ClassificationTreeNode> getClassificationTree() {
        // 1. 从数据库获取所有分类数据 (假设返回的是实体列表)
        List<IndustryClassificationEntity> allList = industryClassificationDao.selectList(null);

        // 2. 创建根节点 "全部"
        DataBasePageInitInfoVO.ClassificationTreeNode rootAll = new DataBasePageInitInfoVO.ClassificationTreeNode("all", "全部");

        // 3. 查找 "生物医用材料（科学）" 节点，构建其子树
        // 注意：这里硬编码了 ID 'bio_med_science'，因为它是树构建的入口
        Optional<IndustryClassificationEntity> sciRootOpt = allList.stream()
                .filter(item -> "bio_med_science".equals(item.getId()))
                .findFirst();

        if (sciRootOpt.isPresent()) {
            IndustryClassificationEntity sciRootEntity = sciRootOpt.get();
            DataBasePageInitInfoVO.ClassificationTreeNode sciRootNode = new DataBasePageInitInfoVO.ClassificationTreeNode(sciRootEntity.getId(), sciRootEntity.getName());

            // 递归构建 "科学" 分支的子节点
            buildTree(sciRootNode, allList);

            // 将构建好的科学分支添加到 "全部" 下面
            rootAll.addChild(sciRootNode);
        }

        // 4. 如果有其他顶级节点，也可以在这里继续添加...

        // 5. 封装成 List 返回
        List<DataBasePageInitInfoVO.ClassificationTreeNode> result = new ArrayList<>();
        result.add(rootAll);
        return result;
    }

    /**
     * 递归构建树形结构
     * @param parentNode 当前父节点对象
     * @param allNodes   数据库全量列表
     */
    private void buildTree(DataBasePageInitInfoVO.ClassificationTreeNode parentNode, List<IndustryClassificationEntity> allNodes) {
        // 找出所有 parent_id 等于当前节点 id 的子节点
        List<IndustryClassificationEntity> children = allNodes.stream()
                .filter(node -> Objects.equals(node.getParentId(), parentNode.getId()))
                .collect(Collectors.toList());

        for (IndustryClassificationEntity child : children) {
            DataBasePageInitInfoVO.ClassificationTreeNode childNode = new DataBasePageInitInfoVO.ClassificationTreeNode(child.getId(), child.getName());
            parentNode.addChild(childNode);

            // 递归调用，继续找下一层
            buildTree(childNode, allNodes);
        }
    }




    @Override
    public Result<DatasetQueryResponse> queryDatasets(DatasetQueryRequest request) {
        try {
            if (request == null) {
                return Result.fail("请求参数不能为空");
            }
            
            Integer page = request.getPage() != null ? request.getPage() : 1;
            Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            
            // 构建查询条件
            LambdaQueryWrapper<ManuDatasetEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ManuDatasetEntity::getDeleted, 0)
                   .eq(ManuDatasetEntity::getIsMenu, 0)  // 只查询数据集，不是目录
                   .eq(ManuDatasetEntity::getAuditStatus, 1);  // 只查询审核通过的
            
            // 数据集名称模糊查询
            if (StringUtils.hasText(request.getDatasetName())) {
                wrapper.like(ManuDatasetEntity::getName, request.getDatasetName().trim());
            }
            
            // 创建人模糊查询
            if (StringUtils.hasText(request.getCreator())) {
                wrapper.like(ManuDatasetEntity::getCreator, request.getCreator().trim());
            }
            
            List<ManuDatasetEntity> datasetList = manuDatasetDao.selectList(wrapper);
            
            // 内存过滤其他条件（行业分类、数据分类、模板名称、部门等）
            List<ManuDatasetEntity> filteredList = datasetList.stream()
                .filter(dataset -> {
                    // 行业分类过滤
                    if (StringUtils.hasText(request.getIndustryCategory())) {
                        // TODO: 需要根据行业分类 ID 过滤，暂时先不过滤
                    }
                    
                    // 数据分类过滤
                    if (StringUtils.hasText(request.getDataCategory())) {
                        // TODO: 需要根据数据分类过滤，暂时先不过滤
                    }
                    
                    // 模板名称过滤
                    if (StringUtils.hasText(request.getTemplateName())) {
                        // 查询模块名称进行匹配
                        ModuleEntity module = moduleDao.selectById(dataset.getModule());
                        if (module == null || !module.getModuleName().contains(request.getTemplateName().trim())) {
                            return false;
                        }
                    }
                    
                    // 部门过滤
                    if (StringUtils.hasText(request.getDepartment())) {
                        // 查询创建人所属部门
                        UserDepartmentEntity userDept = userDepartmentDao.selectOne(
                            new LambdaQueryWrapper<UserDepartmentEntity>()
                                .eq(UserDepartmentEntity::getUsername, dataset.getCreator())
                                .last("limit 1")
                        );
                        if (userDept == null || !userDept.getDepartment().equals(request.getDepartment())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
            
            // 计算分页
            long total = filteredList.size();
            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, (int) total);
            
            List<ManuDatasetEntity> pagedList = fromIndex < total
                ? filteredList.subList(fromIndex, toIndex)
                : new ArrayList<>();
            
            // 转换为响应对象
            List<DatasetQueryResponse.DatasetItem> itemList = pagedList.stream()
                .map(this::convertToDatasetItem)
                .collect(Collectors.toList());
            
            DatasetQueryResponse response = new DatasetQueryResponse();
            response.setTotal(total);
            response.setList(itemList);
            
            return Result.success("查询成功", response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 将 ManuDatasetEntity 转换为 DatasetItem
     */
    private DatasetQueryResponse.DatasetItem convertToDatasetItem(ManuDatasetEntity dataset) {
        DatasetQueryResponse.DatasetItem item = new DatasetQueryResponse.DatasetItem();
        
        item.setId(dataset.getId());
        item.setDatasetName(dataset.getName());
        item.setCreator(dataset.getCreator());
        item.setCreateTime(dataset.getCreateTime());
        item.setUpdateTime(dataset.getCreateTime()); // 暂时没有 updateTime 字段，先用 createTime
        
        // 获取模块信息
        if (dataset.getModule() != null) {
            ModuleEntity module = moduleDao.selectById(dataset.getModule());
            if (module != null) {
                item.setTemplateName(module.getModuleName());
                // 使用 module 的 tag 作为科学分类
                if (StringUtils.hasText(module.getTag())) {
                    item.setScienceCategories(List.of(module.getTag()));
                }
            }
        }
        
        // 获取部门信息
        UserDepartmentEntity userDept = userDepartmentDao.selectOne(
            new LambdaQueryWrapper<UserDepartmentEntity>()
                .eq(UserDepartmentEntity::getUsername, dataset.getCreator())
                .last("limit 1")
        );
        if (userDept != null && StringUtils.hasText(userDept.getDepartment())) {
            DepartmentEntity dept = departmentDao.selectOne(
                new LambdaQueryWrapper<DepartmentEntity>()
                    .eq(DepartmentEntity::getDepartmentName, userDept.getDepartment())
                    .last("limit 1")
            );
            if (dept != null) {
                item.setDepartment(dept.getDepartmentName());
            }
        }
        
        // 设置默认值
        item.setIndustryCategories(new ArrayList<>());
        item.setProductCodes(List.of("-"));
        item.setDataLevel("公益");
        item.setDataCount(0); // TODO: 需要统计数据条数
        item.setDataCategory("数据集");
        item.setFavorited(false); // TODO: 需要查询收藏状态
        
        return item;
    }

    @Override
    public Result<DatasetDetailVO> getDatasetDetail(Integer id) {
        try {
            if (id == null) {
                return Result.fail("数据集 ID 不能为空");
            }
            
            // 查询数据集基本信息
            ManuDatasetEntity dataset = manuDatasetDao.selectById(id);
            if (dataset == null) {
                return Result.fail("数据集不存在");
            }
            
            if (dataset.getDeleted() == 1) {
                return Result.fail("数据集已删除");
            }
            
            // 构建响应对象
            DatasetDetailVO vo = new DatasetDetailVO();
            vo.setId(dataset.getId());
            vo.setDatasetName(dataset.getName());
            vo.setCreator(dataset.getCreator());
            vo.setCreateTime(dataset.getCreateTime());
            vo.setUpdateTime(dataset.getCreateTime()); // 暂时没有 updateTime 字段，先用 createTime
            
            // 获取模块信息（模板名称和科学分类）
            if (dataset.getModule() != null) {
                ModuleEntity module = moduleDao.selectById(dataset.getModule());
                if (module != null) {
                    vo.setTemplateName(module.getModuleName());
                    // 使用 module 的 tag 作为科学分类
                    if (StringUtils.hasText(module.getTag())) {
                        vo.setScienceCategories(List.of(module.getTag()));
                    }
                }
            }
            
            // 获取部门信息
            UserDepartmentEntity userDept = userDepartmentDao.selectOne(
                new LambdaQueryWrapper<UserDepartmentEntity>()
                    .eq(UserDepartmentEntity::getUsername, dataset.getCreator())
                    .last("limit 1")
            );
            if (userDept != null && StringUtils.hasText(userDept.getDepartment())) {
                DepartmentEntity dept = departmentDao.selectOne(
                    new LambdaQueryWrapper<DepartmentEntity>()
                        .eq(DepartmentEntity::getDepartmentName, userDept.getDepartment())
                        .last("limit 1")
                );
                if (dept != null) {
                    vo.setDepartment(dept.getDepartmentName());
                }
            }
            
            // TODO: 行业分类和产品代码需要根据实际业务逻辑获取
            // 暂时设置为空列表
            vo.setIndustryCategories(new ArrayList<>());
            vo.setProductCodes(new ArrayList<>());
            
            // 获取数据条数
            // 需要通过 datasetColumnDao 查询该数据集的所有列，然后统计数据条数
            // 这里暂时设置为 0，后续需要实现具体的统计逻辑
            vo.setDataCount(0);
            
            // 设置默认值
            vo.setDataCategory("数据集");
            vo.setDataLevel("公益");
            
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取数据集详情失败：" + e.getMessage());
        }
    }

    @Override
    public Result<DatasetRecordQueryResponse> queryDatasetRecords(Integer id, DatasetRecordQueryRequest request) {
        try {
            if (id == null) {
                return Result.fail("数据集 ID 不能为空");
            }
            
            if (request == null) {
                return Result.fail("请求参数不能为空");
            }
            
            // 验证数据集是否存在
            ManuDatasetEntity dataset = manuDatasetDao.selectById(id);
            if (dataset == null || dataset.getDeleted() == 1) {
                return Result.fail("数据集不存在");
            }
            
            Integer page = request.getPage() != null ? request.getPage() : 1;
            Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
            
            // 查询该数据集的所有列
            LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
            columnWrapper.eq(DatasetColumnEntity::getDatasetName, dataset.getName())
                        .eq(DatasetColumnEntity::getDeleted, 0);
            List<DatasetColumnEntity> columns = datasetColumnDao.selectList(columnWrapper);
            
            if (columns.isEmpty()) {
                DatasetRecordQueryResponse emptyResponse = new DatasetRecordQueryResponse();
                emptyResponse.setTotal(0L);
                emptyResponse.setList(new ArrayList<>());
                return Result.success(emptyResponse);
            }
            
            // 获取所有列 ID
            List<Integer> columnIds = columns.stream()
                .map(DatasetColumnEntity::getId)
                .collect(Collectors.toList());
            
            // 统计总记录数（去重后的 row_id 数量）
            Long total = datasetDataDao.countDistinctRowIds(columnIds);
            
            if (total == 0) {
                DatasetRecordQueryResponse emptyResponse = new DatasetRecordQueryResponse();
                emptyResponse.setTotal(0L);
                emptyResponse.setList(new ArrayList<>());
                return Result.success(emptyResponse);
            }
            
            // 计算分页参数
            int offset = (page - 1) * pageSize;
            
            // 查询分页的 row_id
            List<Integer> pagedRowIds = datasetDataDao.selectPagedRowIds(columnIds, offset, pageSize);
            
            // 根据 row_id 查询所有列的数据
            List<DatasetDataEntity> dataList = datasetDataDao.selectByColumnIdsAndRowIds(columnIds, pagedRowIds);
            
            // 按 row_id 分组数据
            Map<Integer, List<DatasetDataEntity>> dataMap = dataList.stream()
                .collect(Collectors.groupingBy(DatasetDataEntity::getRowId));
            
            // 构建列名到字段的映射（假设我们知道哪些列对应哪些字段）
            // 这里需要根据实际的 column_name 来映射
            Map<String, Integer> columnNameToFieldMap = new HashMap<>();
            for (DatasetColumnEntity column : columns) {
                columnNameToFieldMap.put(column.getColumnName(), column.getId());
            }
            
            // 转换为响应对象
            List<DatasetRecordQueryResponse.RecordItem> itemList = new ArrayList<>();
            for (Integer rowId : pagedRowIds) {
                List<DatasetDataEntity> rowData = dataMap.get(rowId);
                if (rowData != null && !rowData.isEmpty()) {
                    DatasetRecordQueryResponse.RecordItem item = convertToRecordItem(rowId, rowData, columnNameToFieldMap);
                    itemList.add(item);
                }
            }
            
            DatasetRecordQueryResponse response = new DatasetRecordQueryResponse();
            response.setTotal(total);
            response.setList(itemList);
            
            return Result.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("查询数据集记录失败：" + e.getMessage());
        }
    }
    
    /**
     * 将行数据转换为 RecordItem
     */
    private DatasetRecordQueryResponse.RecordItem convertToRecordItem(
            Integer rowId, 
            List<DatasetDataEntity> rowData,
            Map<String, Integer> columnNameToFieldMap) {
        
        DatasetRecordQueryResponse.RecordItem item = new DatasetRecordQueryResponse.RecordItem();
        item.setRecordId(String.valueOf(rowId));
        
        // 根据列名映射字段值
        // 注意：这里的列名映射是示例，实际需要根据数据库中的真实列名调整
        item.setMaterialCode(getFieldValue(rowData, "material_code", columnNameToFieldMap));
        item.setCollector(getFieldValue(rowData, "collector", columnNameToFieldMap));
        item.setCollectorOrg(getFieldValue(rowData, "collector_org", columnNameToFieldMap));
        item.setCollectDate(getFieldValue(rowData, "collect_date", columnNameToFieldMap));
        item.setDoi(getFieldValue(rowData, "doi", columnNameToFieldMap));
        item.setExperimenter(getFieldValue(rowData, "experimenter", columnNameToFieldMap));
        
        return item;
    }
    
    /**
     * 根据列名获取字段值
     */
    private String getFieldValue(List<DatasetDataEntity> rowData, 
                                  String columnName,
                                  Map<String, Integer> columnNameToFieldMap) {
        Integer columnId = columnNameToFieldMap.get(columnName);
        if (columnId == null) {
            return "-";
        }
        
        return rowData.stream()
            .filter(data -> data.getColumnId().equals(columnId))
            .findFirst()
            .map(DatasetDataEntity::getData)
            .orElse("-");
    }

    @Override
    public Result<FavoriteResponse> favoriteDataset(Integer id, FavoriteRequest request) {
        try {
            if (id == null) {
                return Result.fail("数据集 ID 不能为空");
            }

            if (request == null || request.getFavorited() == null) {
                return Result.fail("请求参数不能为空");
            }

            // 验证数据集是否存在
            ManuDatasetEntity dataset = manuDatasetDao.selectById(id);
            if (dataset == null || dataset.getDeleted() == 1) {
                return Result.fail("数据集不存在");
            }


            String username = UserContext.getCurrentUserName();

            // 查询是否已有收藏记录
            LambdaQueryWrapper<FavoriteEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FavoriteEntity::getUsername, username)
                    .eq(FavoriteEntity::getDatasetId, id)
                    .eq(FavoriteEntity::getDeleted, 0);

            FavoriteEntity existingFavorite = favoriteDao.selectOne(queryWrapper);

            if (existingFavorite != null) {
                // 已有记录，更新状态
                existingFavorite.setFavorited(request.getFavorited());
                existingFavorite.setUpdateTime(LocalDateTime.now());
                favoriteDao.updateById(existingFavorite);
            } else {
                // 没有记录，如果是收藏操作则插入新记录
                if (request.getFavorited()) {
                    FavoriteEntity newFavorite = new FavoriteEntity();
                    newFavorite.setUsername(username);
                    newFavorite.setDatasetId(id);
                    newFavorite.setFavorited(true);
                    newFavorite.setCreateTime(LocalDateTime.now());
                    newFavorite.setUpdateTime(LocalDateTime.now());
                    newFavorite.setDeleted(0);
                    favoriteDao.insert(newFavorite);
                }
            }

            // 构建响应
            FavoriteResponse response = new FavoriteResponse();
            response.setId(id);
            response.setFavorited(request.getFavorited());

            return Result.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("收藏操作失败：" + e.getMessage());
        }
    }


    @Override
    public void downloadDataset(Long id, DownloadRequest request, HttpServletResponse response) {
        String moduleName =
        moduleService.getModuleBaseInfo(
                id.intValue()).getData().getModuleName();
        datasetService.exportDatasetTemplate(
                moduleName
                , response);
    }

    @Override
    public Result<DownloadFieldVO> getDownloadFields(Long id) {
        try {
            if (id == null) {
                return Result.fail("数据集 ID 不能为空");
            }
                
            // 验证数据集是否存在
            ManuDatasetEntity dataset = manuDatasetDao.selectById(id.intValue());
            if (dataset == null || dataset.getDeleted() == 1) {
                return Result.fail("数据集不存在");
            }
                
            // 查询该数据集的所有列配置
            LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
            columnWrapper.eq(DatasetColumnEntity::getDatasetName, dataset.getName())
                        .eq(DatasetColumnEntity::getDeleted, 0)
                        .orderByAsc(DatasetColumnEntity::getId);
            List<DatasetColumnEntity> columns = datasetColumnDao.selectList(columnWrapper);
                
            // 构建响应对象
            DownloadFieldVO vo = new DownloadFieldVO();
            List<DownloadFieldVO.FieldItem> fieldItems = new ArrayList<>();
                
            if (columns != null && !columns.isEmpty()) {
                for (DatasetColumnEntity column : columns) {
                    DownloadFieldVO.FieldItem item = new DownloadFieldVO.FieldItem();
                    item.setLabel(column.getColumnName()); // 使用列名作为标签
                    item.setValue(column.getColumnName()); // 使用列名作为值（key）
                    fieldItems.add(item);
                }
            }
                
            vo.setFields(fieldItems);
                
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取下载字段失败：" + e.getMessage());
        }
    }


}
