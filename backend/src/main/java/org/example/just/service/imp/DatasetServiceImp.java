package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.just.dao.DatasetColumnDao;
import org.example.just.dao.ManuDatasetDao;
import org.example.just.dao.DatasetDataDao;
import org.example.just.dao.ModuleColumnDao;
import org.example.just.dao.ModuleDao;
import org.example.just.context.UserContext;
import org.example.just.dto.categoryDto.ProductCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ProductCategoryTreeResult;
import org.example.just.dto.categoryDto.ProductCategoryTreeVO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeResult;
import org.example.just.dto.categoryDto.ScienceCategoryTreeVO;
import org.example.just.dto.datasetDto.*;
import org.example.just.entity.DatasetColumnEntity;
import org.example.just.entity.DatasetDataEntity;
import org.example.just.entity.ManuDatasetEntity;
import org.example.just.entity.ModuleColumnEntity;
import org.example.just.entity.ModuleEntity;
import org.example.just.service.DatasetService;
import org.example.just.utils.PageQuery;
import org.example.just.utils.PageResult;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatasetServiceImp implements DatasetService {

    private final ManuDatasetDao DatasetDao;
    private final DatasetColumnDao DatasetColumnDao;
    private final DatasetDataDao DatasetDataDao;
    private final ModuleDao moduleDao;
    private final ModuleColumnDao moduleColumnDao;

    public DatasetServiceImp(ManuDatasetDao DatasetDao,
                            DatasetColumnDao DatasetColumnDao,
                            DatasetDataDao DatasetDataDao,
                             ModuleDao moduleDao,
                             ModuleColumnDao moduleColumnDao) {
        this.DatasetDao = DatasetDao;
        this.DatasetColumnDao = DatasetColumnDao;
        this.DatasetDataDao = DatasetDataDao;
        this.moduleDao = moduleDao;
        this.moduleColumnDao = moduleColumnDao;
    }

    @Transactional
    @Override
    public Result<String> createMenu(CreateMenuDatasetDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            return Result.fail("目录名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCreator())) {
            return Result.fail("创建人不能为空");
        }

        String name = dto.getName().trim();
        String creator = dto.getCreator().trim();
        Integer parentId = dto.getParent();

        // 根目录统一按0处理
        if (parentId == null) {
            parentId = 0;
        }

        // 1. 如果是创建次级目录，先校验父目录是否存在，并且必须是目录
        if (parentId != 0) {
            LambdaQueryWrapper<ManuDatasetEntity> parentWrapper = new LambdaQueryWrapper<>();
            parentWrapper.eq(ManuDatasetEntity::getId, parentId)
                    .eq(ManuDatasetEntity::getIsMenu, 1)
                    .eq(ManuDatasetEntity::getDeleted, 0)
                    .last("limit 1");

            ManuDatasetEntity parentDataset = DatasetDao.selectOne(parentWrapper);
            if (parentDataset == null) {
                return Result.fail("父级目录不存在");
            }
        }

        // 2. 校验同级目录名称不能重复
        LambdaQueryWrapper<ManuDatasetEntity> sameLevelWrapper = new LambdaQueryWrapper<>();
        sameLevelWrapper.eq(ManuDatasetEntity::getName, name)
                .eq(ManuDatasetEntity::getIsMenu, 1)
                .eq(ManuDatasetEntity::getParent, parentId)
                .eq(ManuDatasetEntity::getDeleted, 0);

        if (DatasetDao.selectCount(sameLevelWrapper) > 0) {
            return Result.fail("同级目录下已存在同名目录");
        }

        // 3. 插入目录
        ManuDatasetEntity dataset = new ManuDatasetEntity();
        dataset.setName(name);
        dataset.setCreator(creator);
        dataset.setCreateTime(LocalDateTime.now());
        dataset.setParent(parentId);
        dataset.setIsMenu(1);
        dataset.setDeleted(0);

        int rows = DatasetDao.insert(dataset);
        if (rows <= 0) {
            return Result.fail("创建目录失败");
        }

        return Result.success("创建目录成功");
    }

    @Override
    public Result<List<ManuDatasetTreeVO>> getDatasetTree() {
        LambdaQueryWrapper<ManuDatasetEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManuDatasetEntity::getDeleted, 0)
                .orderByAsc(ManuDatasetEntity::getId);

        List<ManuDatasetEntity> datasetList = DatasetDao.selectList(wrapper);

        if (datasetList == null || datasetList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 先转成 VO
        List<ManuDatasetTreeVO> voList = datasetList.stream().map(dataset -> {
            ManuDatasetTreeVO vo = new ManuDatasetTreeVO();
            vo.setId(dataset.getId());
            vo.setName(dataset.getName());
            vo.setCreator(dataset.getCreator());
            vo.setCreateTime(dataset.getCreateTime());
            vo.setParent(dataset.getParent());
            vo.setIsMenu(dataset.getIsMenu());
            return vo;
        }).collect(Collectors.toList());

        // 用 id 建索引
        Map<Integer, ManuDatasetTreeVO> datasetMap = voList.stream()
                .collect(Collectors.toMap(ManuDatasetTreeVO::getId, item -> item, (a, b) -> a));

        List<ManuDatasetTreeVO> rootList = new ArrayList<>();

        for (ManuDatasetTreeVO node : voList) {
            Integer parentId = node.getParent();

            // parent为0或null，表示根节点
            if (parentId == null || parentId == 0) {
                rootList.add(node);
            } else {
                ManuDatasetTreeVO parentNode = datasetMap.get(parentId);
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                } else {
                    // 父节点不存在时兜底挂到根节点，避免数据丢失
                    rootList.add(node);
                }
            }
        }

        return Result.success(rootList);
    }

    @Override
    public ScienceCategoryTreeResult getScienceCategoryTree(ScienceCategoryTreeQueryDTO query) {
        String keyword = query != null && StringUtils.hasText(query.getKeyword())
                ? query.getKeyword().trim()
                : "";
        int page = query == null || query.getPage() == null || query.getPage() < 1
                ? 1
                : query.getPage();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 50
                : query.getPageSize();

        LambdaQueryWrapper<ManuDatasetEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManuDatasetEntity::getDeleted, 0)
                .orderByAsc(ManuDatasetEntity::getId);

        List<ManuDatasetEntity> allList = DatasetDao.selectList(wrapper);
        if (allList == null || allList.isEmpty()) {
            return ScienceCategoryTreeResult.success(new ArrayList<>(), 0);
        }

        List<ManuDatasetEntity> categoryList = allList.stream()
                .filter(item -> item.getIsMenu() != null && item.getIsMenu() == 1)
                .collect(Collectors.toList());
        if (categoryList.isEmpty()) {
            return ScienceCategoryTreeResult.success(new ArrayList<>(), 0);
        }

        Map<Integer, ManuDatasetEntity> categoryMap = categoryList.stream()
                .collect(Collectors.toMap(ManuDatasetEntity::getId, item -> item, (a, b) -> a));
        Map<Integer, List<ManuDatasetEntity>> categoryChildrenMap = new HashMap<>();
        for (ManuDatasetEntity item : categoryList) {
            Integer parentId = item.getParent() == null ? 0 : item.getParent();
            categoryChildrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
        }
        Map<Integer, List<ManuDatasetEntity>> allChildrenMap = new HashMap<>();
        for (ManuDatasetEntity item : allList) {
            Integer parentId = item.getParent() == null ? 0 : item.getParent();
            allChildrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
        }

        Map<Integer, Integer> datasetCountCache = new HashMap<>();
        List<ScienceCategoryTreeVO> roots = categoryList.stream()
                .filter(item -> item.getParent() == null
                        || item.getParent() == 0
                        || !categoryMap.containsKey(item.getParent()))
                .map(item -> buildScienceCategoryNode(item, 0, categoryChildrenMap, allChildrenMap, datasetCountCache))
                .map(item -> filterScienceCategoryTree(item, keyword))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int total = roots.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        return ScienceCategoryTreeResult.success(new ArrayList<>(roots.subList(fromIndex, toIndex)), total);
    }

    private ScienceCategoryTreeVO buildScienceCategoryNode(ManuDatasetEntity entity,
                                                           int level,
                                                           Map<Integer, List<ManuDatasetEntity>> categoryChildrenMap,
                                                           Map<Integer, List<ManuDatasetEntity>> allChildrenMap,
                                                           Map<Integer, Integer> datasetCountCache) {
        ScienceCategoryTreeVO vo = new ScienceCategoryTreeVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setLevel(level);
        vo.setDatasetCount(countDatasetsUnderCategory(entity.getId(), allChildrenMap, datasetCountCache));
        vo.setTemplateCount(0);

        List<ManuDatasetEntity> children = categoryChildrenMap.get(entity.getId());
        if (children != null && !children.isEmpty()) {
            List<ScienceCategoryTreeVO> childVOList = children.stream()
                    .map(child -> buildScienceCategoryNode(child, level + 1, categoryChildrenMap, allChildrenMap, datasetCountCache))
                    .collect(Collectors.toList());
            vo.setChildren(childVOList);
        }
        return vo;
    }

    private int countDatasetsUnderCategory(Integer categoryId,
                                           Map<Integer, List<ManuDatasetEntity>> allChildrenMap,
                                           Map<Integer, Integer> datasetCountCache) {
        if (categoryId == null) {
            return 0;
        }
        Integer cached = datasetCountCache.get(categoryId);
        if (cached != null) {
            return cached;
        }

        int count = 0;
        List<ManuDatasetEntity> children = allChildrenMap.get(categoryId);
        if (children != null) {
            for (ManuDatasetEntity child : children) {
                if (child.getIsMenu() != null && child.getIsMenu() == 1) {
                    count += countDatasetsUnderCategory(child.getId(), allChildrenMap, datasetCountCache);
                } else {
                    count++;
                }
            }
        }
        datasetCountCache.put(categoryId, count);
        return count;
    }

    private ScienceCategoryTreeVO filterScienceCategoryTree(ScienceCategoryTreeVO node, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return node;
        }

        boolean selfMatched = node.getName() != null && node.getName().contains(keyword);
        List<ScienceCategoryTreeVO> matchedChildren = node.getChildren().stream()
                .map(child -> filterScienceCategoryTree(child, keyword))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!selfMatched && matchedChildren.isEmpty()) {
            return null;
        }
        if (!selfMatched) {
            node.setChildren(matchedChildren);
        }
        return node;
    }

    @Override
    public ProductCategoryTreeResult getProductCategoryTree(ProductCategoryTreeQueryDTO query) {
        List<String> keywords = new ArrayList<>();
        if (query != null && StringUtils.hasText(query.getIndustryKeyword())) {
            keywords.add(query.getIndustryKeyword().trim());
        }
        if (query != null && StringUtils.hasText(query.getSectorKeyword())) {
            keywords.add(query.getSectorKeyword().trim());
        }
        if (query != null && StringUtils.hasText(query.getProductKeyword())) {
            keywords.add(query.getProductKeyword().trim());
        }

        LambdaQueryWrapper<ManuDatasetEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManuDatasetEntity::getDeleted, 0)
                .orderByAsc(ManuDatasetEntity::getId);

        List<ManuDatasetEntity> allList = DatasetDao.selectList(wrapper);
        if (allList == null || allList.isEmpty()) {
            return ProductCategoryTreeResult.success(new ArrayList<>(), 0);
        }

        List<ManuDatasetEntity> categoryList = allList.stream()
                .filter(item -> item.getIsMenu() != null && item.getIsMenu() == 1)
                .collect(Collectors.toList());
        if (categoryList.isEmpty()) {
            return ProductCategoryTreeResult.success(new ArrayList<>(), 0);
        }

        Map<Integer, ManuDatasetEntity> categoryMap = categoryList.stream()
                .collect(Collectors.toMap(ManuDatasetEntity::getId, item -> item, (a, b) -> a));
        Map<Integer, List<ManuDatasetEntity>> categoryChildrenMap = new HashMap<>();
        for (ManuDatasetEntity item : categoryList) {
            Integer parentId = item.getParent() == null ? 0 : item.getParent();
            categoryChildrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
        }

        List<ProductCategoryTreeVO> roots = categoryList.stream()
                .filter(item -> item.getParent() == null
                        || item.getParent() == 0
                        || !categoryMap.containsKey(item.getParent()))
                .map(item -> buildProductCategoryNode(item, "", categoryChildrenMap))
                .map(item -> filterProductCategoryTree(item, keywords))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ProductCategoryTreeResult.success(roots, roots.size());
    }

    private ProductCategoryTreeVO buildProductCategoryNode(ManuDatasetEntity entity,
                                                           String parentCode,
                                                           Map<Integer, List<ManuDatasetEntity>> categoryChildrenMap) {
        String currentCode = StringUtils.hasText(parentCode)
                ? parentCode + "." + entity.getId()
                : String.valueOf(entity.getId());
        ProductCategoryTreeVO vo = new ProductCategoryTreeVO();
        vo.setId("p" + entity.getId());
        vo.setIndustryCode(currentCode);
        vo.setIndustryName(entity.getName());
        vo.setSectorCode("");
        vo.setSectorName("");
        vo.setProductCode("");
        vo.setProductName("");

        List<ManuDatasetEntity> children = categoryChildrenMap.get(entity.getId());
        if (children != null && !children.isEmpty()) {
            List<ProductCategoryTreeVO> childVOList = children.stream()
                    .map(child -> buildProductCategoryNode(child, currentCode, categoryChildrenMap))
                    .collect(Collectors.toList());
            vo.setChildren(childVOList);
        }
        return vo;
    }

    private ProductCategoryTreeVO filterProductCategoryTree(ProductCategoryTreeVO node, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return node;
        }

        boolean selfMatched = keywords.stream().allMatch(keyword -> productCategoryNodeMatches(node, keyword));
        List<ProductCategoryTreeVO> matchedChildren = node.getChildren().stream()
                .map(child -> filterProductCategoryTree(child, keywords))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!selfMatched && matchedChildren.isEmpty()) {
            return null;
        }
        if (!selfMatched) {
            node.setChildren(matchedChildren);
        }
        return node;
    }

    private boolean productCategoryNodeMatches(ProductCategoryTreeVO node, String keyword) {
        return containsKeyword(node.getIndustryCode(), keyword)
                || containsKeyword(node.getIndustryName(), keyword)
                || containsKeyword(node.getSectorCode(), keyword)
                || containsKeyword(node.getSectorName(), keyword)
                || containsKeyword(node.getProductCode(), keyword)
                || containsKeyword(node.getProductName(), keyword);
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    @Override
    public DatasetOptionsResult getDatasetOptions(DatasetOptionsQueryDTO query) {
        List<Integer> scienceCategoryIds = query == null || query.getScienceCategoryIds() == null
                ? new ArrayList<>()
                : query.getScienceCategoryIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        String keyword = query != null && StringUtils.hasText(query.getKeyword())
                ? query.getKeyword().trim()
                : "";
        int page = query == null || query.getPage() == null || query.getPage() < 1
                ? 1
                : query.getPage();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 20
                : query.getPageSize();

        LambdaQueryWrapper<ManuDatasetEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ManuDatasetEntity::getDeleted, 0)
                .eq(ManuDatasetEntity::getIsMenu, 0);
        if (!scienceCategoryIds.isEmpty()) {
            wrapper.in(ManuDatasetEntity::getScienceCategoryId, scienceCategoryIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ManuDatasetEntity::getName, keyword);
        }
        wrapper.orderByAsc(ManuDatasetEntity::getId);

        List<ManuDatasetEntity> datasets = DatasetDao.selectList(wrapper);
        if (datasets == null || datasets.isEmpty()) {
            return DatasetOptionsResult.success(new ArrayList<>(), 0);
        }

        datasets = datasets.stream()
                .filter(dataset -> dataset.getDeleted() == null || dataset.getDeleted() == 0)
                .filter(dataset -> dataset.getIsMenu() != null && dataset.getIsMenu() == 0)
                .filter(dataset -> scienceCategoryIds.isEmpty()
                        || scienceCategoryIds.contains(dataset.getScienceCategoryId()))
                .filter(dataset -> !StringUtils.hasText(keyword)
                        || (dataset.getName() != null && dataset.getName().contains(keyword)))
                .collect(Collectors.toList());

        int total = datasets.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<DatasetOptionsVO> options = datasets.subList(fromIndex, toIndex).stream()
                .map(dataset -> {
                    DatasetOptionsVO vo = new DatasetOptionsVO();
                    vo.setId(dataset.getId());
                    vo.setName(dataset.getName());
                    vo.setDataLevel(dataset.getDataLevel());
                    vo.setRecordCount(countDatasetRecords(dataset.getName()));
                    return vo;
                })
                .collect(Collectors.toList());

        return DatasetOptionsResult.success(options, total);
    }

    private Long countDatasetRecords(String datasetName) {
        if (!StringUtils.hasText(datasetName)) {
            return 0L;
        }

        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, datasetName)
                .eq(DatasetColumnEntity::getDeleted, 0)
                .orderByAsc(DatasetColumnEntity::getId);
        List<DatasetColumnEntity> columns = DatasetColumnDao.selectList(columnWrapper);
        if (columns == null || columns.isEmpty()) {
            return 0L;
        }

        List<Integer> columnIds = columns.stream()
                .map(DatasetColumnEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (columnIds.isEmpty()) {
            return 0L;
        }

        Long count = DatasetDataDao.countDistinctRowIds(columnIds);
        return count == null ? 0L : count;
    }

    @Override
    public Result<OnlineFormSchemaVO> getOnlineFormSchema(OnlineFormSchemaQueryDTO query) {
        if (query == null || query.getDatasetId() == null) {
            return Result.fail("数据集ID不能为空");
        }

        LambdaQueryWrapper<ManuDatasetEntity> datasetWrapper = new LambdaQueryWrapper<>();
        datasetWrapper.eq(ManuDatasetEntity::getId, query.getDatasetId())
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .eq(ManuDatasetEntity::getDeleted, 0)
                .last("limit 1");
        ManuDatasetEntity dataset = DatasetDao.selectOne(datasetWrapper);
        if (dataset == null) {
            return Result.fail("数据集不存在");
        }

        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, dataset.getName())
                .eq(DatasetColumnEntity::getDeleted, 0)
                .orderByAsc(DatasetColumnEntity::getId);
        List<DatasetColumnEntity> columns = DatasetColumnDao.selectList(columnWrapper);
        if (columns == null) {
            columns = new ArrayList<>();
        }

        OnlineFormSectionVO section = new OnlineFormSectionVO();
        section.setId("object");
        section.setTitle("对象区域");
        section.setSubtitle("");

        List<OnlineFormFieldVO> fields = new ArrayList<>();
        int dataSourceInsertIndex = columns.isEmpty() ? 0 : 1;
        for (int i = 0; i < columns.size(); i++) {
            if (i == dataSourceInsertIndex) {
                fields.add(buildDataSourceField());
            }
            fields.add(buildOnlineFormField(columns.get(i), i));
        }
        if (columns.size() <= dataSourceInsertIndex) {
            fields.add(buildDataSourceField());
        }
        section.setFields(fields);

        OnlineFormSchemaVO schema = new OnlineFormSchemaVO();
        schema.setSections(List.of(section));
        return Result.success(0, "success", schema);
    }

    private OnlineFormFieldVO buildOnlineFormField(DatasetColumnEntity column, int index) {
        OnlineFormFieldVO field = new OnlineFormFieldVO();
        field.setId(column.getId() == null ? "field" + (index + 1) : "column" + column.getId());
        field.setLabel(column.getColumnName());
        field.setType(toOnlineFormFieldType(column.getColumnType()));
        field.setRequired(true);
        field.setPlaceholder("");
        field.setDescription("");
        if ("select".equals(field.getType())) {
            field.setOptions(new ArrayList<>());
        }
        return field;
    }

    private OnlineFormFieldVO buildDataSourceField() {
        OnlineFormFieldVO field = new OnlineFormFieldVO();
        field.setId("dataSource");
        field.setLabel("数据来源");
        field.setType("select");
        field.setRequired(true);
        field.setPlaceholder("请选择");
        field.setDescription("选取一种类型以生成对应的表单");
        field.setOptions(List.of(
                new OnlineFormFieldOptionVO("experiment", "实验测量"),
                new OnlineFormFieldOptionVO("simulation", "数值模拟")
        ));
        return field;
    }

    private String toOnlineFormFieldType(String columnType) {
        if (!StringUtils.hasText(columnType)) {
            return "text";
        }
        String normalizedType = columnType.trim().toLowerCase(Locale.ROOT);
        if (normalizedType.contains("select") || normalizedType.contains("enum")) {
            return "select";
        }
        if (normalizedType.contains("textarea") || normalizedType.contains("longtext")) {
            return "textarea";
        }
        return "text";
    }

    @Transactional
    @Override
    public Result<CreateDatasetResultVO> createDatasetForApi(CreateDatasetDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            return Result.fail("数据集名称不能为空");
        }
        if (dto.getName().trim().length() > 200) {
            return Result.fail("数据集名称不能超过200字");
        }
        if (!StringUtils.hasText(dto.getSummary())) {
            return Result.fail("数据集摘要不能为空");
        }
        if (dto.getSummary().trim().length() > 500) {
            return Result.fail("数据集摘要不能超过500字");
        }
        if (dto.getScienceCategoryId() == null) {
            return Result.fail("科学分类不能为空");
        }
        if (dto.getProductCategoryId() == null) {
            return Result.fail("产品分类不能为空");
        }
        if (!StringUtils.hasText(dto.getDataLevel())) {
            return Result.fail("数据级别不能为空");
        }
        if (!StringUtils.hasText(dto.getDataCategory())) {
            return Result.fail("数据类别不能为空");
        }
        if (dto.getTemplateTagId() == null) {
            return Result.fail("模板标签不能为空");
        }
        if (dto.getTemplateId() == null) {
            return Result.fail("模板不能为空");
        }

        String datasetName = dto.getName().trim();
        String summary = dto.getSummary().trim();
        String coverUrl = StringUtils.hasText(dto.getCoverUrl()) ? dto.getCoverUrl().trim() : null;
        String dataLevel = dto.getDataLevel().trim();
        String dataCategory = dto.getDataCategory().trim();
        Integer templateId = dto.getTemplateId();
        String creator = UserContext.getCurrentUserName();
        if (!StringUtils.hasText(creator)) {
            Integer currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail(401, "未登录");
            }
            creator = String.valueOf(currentUserId);
        }
        if (!Set.of("highvalue", "public", "private").contains(dataLevel)) {
            return Result.fail("数据级别不合法");
        }
        if (!"dataset".equals(dataCategory)) {
            return Result.fail("数据类别必须为dataset");
        }

        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getId, templateId)
                .eq(ModuleEntity::getDeleted, 0)
                .last("limit 1");

        ModuleEntity moduleEntity = moduleDao.selectOne(moduleWrapper);
        if (moduleEntity == null) {
            return Result.fail("模板不存在");
        }

        LambdaQueryWrapper<ManuDatasetEntity> datasetWrapper = new LambdaQueryWrapper<>();
        datasetWrapper.eq(ManuDatasetEntity::getName, datasetName)
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .eq(ManuDatasetEntity::getDeleted, 0);

        if (DatasetDao.selectCount(datasetWrapper) > 0) {
            return Result.fail(4001, "数据集名称已存在");
        }

        LambdaQueryWrapper<ModuleColumnEntity> templateColumnWrapper = new LambdaQueryWrapper<>();
        templateColumnWrapper.eq(ModuleColumnEntity::getModuleId, templateId)
                .eq(ModuleColumnEntity::getDeleted, 0)
                .orderByAsc(ModuleColumnEntity::getCreateTime)
                .orderByAsc(ModuleColumnEntity::getId);

        List<ModuleColumnEntity> templateColumns = moduleColumnDao.selectList(templateColumnWrapper);
        if (templateColumns == null || templateColumns.isEmpty()) {
            return Result.fail("模板字段不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        ManuDatasetEntity dataset = new ManuDatasetEntity();
        dataset.setName(datasetName);
        dataset.setSummary(summary);
        dataset.setCoverUrl(coverUrl);
        dataset.setCreator(creator);
        dataset.setCreateTime(now);
        dataset.setParent(0);
        dataset.setIsMenu(0);
        dataset.setDeleted(0);
        dataset.setModule(templateId);
        dataset.setScienceCategoryId(dto.getScienceCategoryId());
        dataset.setProductCategoryId(dto.getProductCategoryId());
        dataset.setDataLevel(dataLevel);
        dataset.setDataCategory(dataCategory);
        dataset.setTemplateTagId(dto.getTemplateTagId());
        dataset.setDatasetTagIds(toJsonArray(dto.getDatasetTagIds()));
        dataset.setAuditStatus(0);

        int datasetRows = DatasetDao.insert(dataset);
        if (datasetRows <= 0) {
            return Result.fail("创建数据集失败");
        }

        for (ModuleColumnEntity templateColumn : templateColumns) {
            if (!StringUtils.hasText(templateColumn.getColumnName())) {
                throw new RuntimeException("模板字段名称不能为空");
            }
            if (!StringUtils.hasText(templateColumn.getType())) {
                throw new RuntimeException("模板字段类型不能为空");
            }
            DatasetColumnEntity datasetColumn = new DatasetColumnEntity();
            datasetColumn.setColumnName(templateColumn.getColumnName().trim());
            datasetColumn.setColumnType(templateColumn.getType().trim());
            datasetColumn.setDatasetName(datasetName);
            datasetColumn.setDeleted(0);

            int columnRows = DatasetColumnDao.insert(datasetColumn);
            if (columnRows <= 0) {
                throw new RuntimeException("插入数据集字段失败");
            }
        }

        CreateDatasetResultVO resultVO = new CreateDatasetResultVO();
        resultVO.setDatasetId(dataset.getId());
        resultVO.setName(datasetName);
        resultVO.setCreatedAt(now);
        return Result.success(0, "success", resultVO);
    }

    private String toJsonArray(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Transactional
    @Override
    public Result<String> importDatasetData(String DatasetName, MultipartFile file) {
        if (!StringUtils.hasText(DatasetName)) {
            return Result.fail("模板名称不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatasetEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatasetEntity::getName, DatasetName.trim())
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .last("limit 1");
        ManuDatasetEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板对应列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName.trim());
        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        Map<String, DatasetColumnEntity> columnMap = columnList.stream()
                .collect(Collectors.toMap(DatasetColumnEntity::getColumnName, item -> item, (a, b) -> a));

        List<Integer> columnIds = columnList.stream()
                .map(DatasetColumnEntity::getId)
                .collect(Collectors.toList());

        Integer maxRowId = DatasetDataDao.selectMaxRowIdByColumnIds(columnIds);
        int nextRowId = (maxRowId == null ? 0 : maxRowId) + 1;

        List<DatasetDataEntity> batchList = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Result.fail("Excel中没有可读取的Sheet");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return Result.fail("Excel表头不能为空");
            }

            // 3. 读取表头，并建立 Excel 列索引 -> 模板列 的映射
            Map<Integer, DatasetColumnEntity> matchedColumnIndexMap = new LinkedHashMap<>();
            int lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i);
                String headerName = dataFormatter.formatCellValue(cell).trim();
                if (!StringUtils.hasText(headerName)) {
                    continue;
                }

                DatasetColumnEntity matchedColumn = columnMap.get(headerName);
                if (matchedColumn != null) {
                    matchedColumnIndexMap.put(i, matchedColumn);
                }
            }

            if (matchedColumnIndexMap.isEmpty()) {
                return Result.fail("Excel表头与模板列没有任何匹配项");
            }

            // 4. 遍历数据行
            int lastRowNum = sheet.getLastRowNum();
            int importedRowCount = 0;

            for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null) {
                    continue;
                }

                boolean rowHasValue = false;
                List<DatasetDataEntity> currentRowDataList = new ArrayList<>();

                for (Map.Entry<Integer, DatasetColumnEntity> entry : matchedColumnIndexMap.entrySet()) {
                    Integer cellIndex = entry.getKey();
                    DatasetColumnEntity DatasetColumn = entry.getValue();

                    Cell cell = dataRow.getCell(cellIndex);
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null) {
                        cellValue = cellValue.trim();
                    }

                    if (StringUtils.hasText(cellValue)) {
                        rowHasValue = true;
                    }

                    DatasetDataEntity DatasetData = new DatasetDataEntity();
                    DatasetData.setColumnId(DatasetColumn.getId());
                    DatasetData.setRowId(nextRowId);
                    DatasetData.setDataType(DatasetColumn.getColumnType());
                    DatasetData.setData(cellValue);
                    DatasetData.setDeleted(0);

                    currentRowDataList.add(DatasetData);
                }

                // 整行都为空，跳过，不占 row_id
                if (!rowHasValue) {
                    continue;
                }

                batchList.addAll(currentRowDataList);
                importedRowCount++;
                nextRowId++;
            }

            if (batchList.isEmpty()) {
                return Result.fail("没有可导入的数据");
            }

            DatasetDataDao.insertBatch(batchList);
            return Result.success("导入成功，共导入 " + importedRowCount + " 行数据");

        } catch (Exception e) {
            throw new RuntimeException("导入模板数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Result<DatasetDataPageVO> getDatasetDataPage(String DatasetName, PageQuery pageQuery) {
        if (!StringUtils.hasText(DatasetName)) {
            return Result.fail("模板名称不能为空");
        }
        if (pageQuery == null) {
            return Result.fail("分页参数不能为空");
        }

        // 1. 校验模板存在，并且必须是模板而不是目录
        LambdaQueryWrapper<ManuDatasetEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatasetEntity::getName, DatasetName.trim())
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .last("limit 1");
        ManuDatasetEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName.trim())
                .orderByAsc(DatasetColumnEntity::getId);

        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        List<Integer> columnIds = columnList.stream()
                .map(DatasetColumnEntity::getId)
                .toList();

        // 3. 先统计总行数
        Long total = DatasetDataDao.countDistinctRowIds(columnIds);
        if (total == null) {
            total = 0L;
        }

        List<DatasetRowVO> rowVOList = new ArrayList<>();

        if (total > 0) {
            // 4. 分页取 row_id
            List<Integer> rowIds = DatasetDataDao.selectPagedRowIds(
                    columnIds,
                    pageQuery.getOffset(),
                    pageQuery.getRealPageSize()
            );

            if (rowIds != null && !rowIds.isEmpty()) {
                // 5. 查这一页 row_id 下的所有单元格数据
                List<DatasetDataEntity> cellList = DatasetDataDao.selectByColumnIdsAndRowIds(columnIds, rowIds);

                // 列ID -> 列定义
                Map<Integer, DatasetColumnEntity> columnMap = columnList.stream()
                        .collect(Collectors.toMap(DatasetColumnEntity::getId, item -> item, (a, b) -> a));

                // row_id -> 当前行数据
                Map<Integer, DatasetRowVO> rowMap = new LinkedHashMap<>();

                // 先按分页顺序建立行，保证返回顺序稳定
                for (Integer rowId : rowIds) {
                    DatasetRowVO rowVO = new DatasetRowVO();
                    rowVO.setRowId(rowId);
                    rowVO.setData(new LinkedHashMap<>());
                    rowMap.put(rowId, rowVO);
                }

                // 把 cell 填充进每一行
                for (DatasetDataEntity cell : cellList) {
                    DatasetRowVO rowVO = rowMap.get(cell.getRowId());
                    DatasetColumnEntity column = columnMap.get(cell.getColumnId());
                    if (rowVO != null && column != null) {
                        rowVO.getData().put(column.getColumnName(), cell.getData());
                    }
                }

                rowVOList = new ArrayList<>(rowMap.values());
            }
        }

        // 6. 组装列定义
        List<DatasetColumnVO> columnVOList = columnList.stream().map(column -> {
            DatasetColumnVO vo = new DatasetColumnVO();
            vo.setId(column.getId());
            vo.setColumnName(column.getColumnName());
            vo.setColumnType(column.getColumnType());
            return vo;
        }).toList();

        // 7. 组装分页结果
        PageResult<DatasetRowVO> pageResult = PageResult.of(
                pageQuery.getRealPageNum(),
                pageQuery.getRealPageSize(),
                total,
                rowVOList
        );

        DatasetDataPageVO resultVO = new DatasetDataPageVO();
        resultVO.setDatasetName(DatasetName.trim());
        resultVO.setColumns(columnVOList);
        resultVO.setPageData(pageResult);

        return Result.success(resultVO);
    }

    @Override
    public void exportDatasetTemplate(String DatasetName, HttpServletResponse response) {
        if (!StringUtils.hasText(DatasetName)) {
            throw new RuntimeException("模板名称不能为空");
        }

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatasetEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatasetEntity::getName, DatasetName.trim())
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatasetEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            throw new RuntimeException("模板不存在");
        }

        // 2. 查询模板列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName.trim())
                .orderByAsc(DatasetColumnEntity::getId);

        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            throw new RuntimeException("当前模板未定义列，无法导出模板结构");
        }

        // 3. 创建 Excel，只写表头，不写数据
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("模板结构");

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columnList.size(); i++) {
                DatasetColumnEntity column = columnList.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.getColumnName());
                sheet.autoSizeColumn(i);
            }

            // 4. 设置响应头
            String fileName = DatasetName.trim() + "_模板结构.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);

            // 5. 输出到前端
            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();

        } catch (Exception e) {
            throw new RuntimeException("导出模板结构失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public Result<String> addDatasetColumn(AddDatasetColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnName())) {
            return Result.fail("列名称不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnType())) {
            return Result.fail("列类型不能为空");
        }

        String DatasetName = dto.getDatasetName().trim();
        String columnName = dto.getColumnName().trim();
        String columnType = dto.getColumnType().trim();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatasetEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatasetEntity::getName, DatasetName)
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatasetEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验当前模板下列名不能重复
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName)
                .eq(DatasetColumnEntity::getColumnName, columnName)
                .last("limit 1");

        DatasetColumnEntity existedColumn = DatasetColumnDao.selectOne(columnWrapper);
        if (existedColumn != null) {
            return Result.fail("当前模板下已存在同名列");
        }

        // 3. 插入模板列
        DatasetColumnEntity DatasetColumn = new DatasetColumnEntity();
        DatasetColumn.setDatasetName(DatasetName);
        DatasetColumn.setColumnName(columnName);
        DatasetColumn.setColumnType(columnType);
        DatasetColumn.setDeleted(0);

        int rows = DatasetColumnDao.insert(DatasetColumn);
        if (rows <= 0) {
            return Result.fail("新增模板列失败");
        }

        return Result.success("新增模板列成功");
    }

    @Transactional
    @Override
    public Result<String> deleteDatasetColumn(DeleteDatabaseColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getColumnId() == null) {
            return Result.fail("模板列ID不能为空");
        }

        // 1. 校验列存在
        DatasetColumnEntity DatasetColumn = DatasetColumnDao.selectById(dto.getColumnId());
        if (DatasetColumn == null) {
            return Result.fail("模板列不存在");
        }

        // 2. 逻辑删除模板列
        int columnRows = DatasetColumnDao.deleteById(dto.getColumnId());
        if (columnRows <= 0) {
            return Result.fail("删除模板列失败");
        }

        // 3. 逻辑删除该列对应的数据
        LambdaUpdateWrapper<DatasetDataEntity> dataUpdateWrapper = new LambdaUpdateWrapper<>();
        dataUpdateWrapper.eq(DatasetDataEntity::getColumnId, dto.getColumnId())
                .set(DatasetDataEntity::getDeleted, 1);

        DatasetDataDao.update(null, dataUpdateWrapper);

        return Result.success("删除模板列成功");
    }

    @Transactional
    @Override
    public Result<String> deleteDatasetRow(DeleteDatasetRowDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("模板名称不能为空");
        }
        if (dto.getRowId() == null) {
            return Result.fail("行ID不能为空");
        }

        String DatasetName = dto.getDatasetName().trim();
        Integer rowId = dto.getRowId();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatasetEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatasetEntity::getName, DatasetName)
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatasetEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板下所有列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName);

        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        List<Integer> columnIds = columnList.stream()
                .map(DatasetColumnEntity::getId)
                .toList();

        // 3. 先检查这一行数据是否存在
        LambdaQueryWrapper<DatasetDataEntity> dataCheckWrapper = new LambdaQueryWrapper<>();
        dataCheckWrapper.eq(DatasetDataEntity::getRowId, rowId)
                .in(DatasetDataEntity::getColumnId, columnIds);

        Long count = DatasetDataDao.selectCount(dataCheckWrapper);
        if (count == null || count == 0) {
            return Result.fail("当前行数据不存在");
        }

        // 4. 逻辑删除这一整行
        LambdaUpdateWrapper<DatasetDataEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DatasetDataEntity::getRowId, rowId)
                .in(DatasetDataEntity::getColumnId, columnIds)
                .set(DatasetDataEntity::getDeleted, 1);

        int rows = DatasetDataDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("删除行数据失败");
        }

        return Result.success("删除行数据成功");
    }


    @Transactional
    @Override
    public Result<AuditDatasetResultVO> auditDataset(AuditDatasetDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("模板名称不能为空");
        }
        if (dto.getStatus() == null) {
            return Result.fail("审核状态不能为空");
        }
        if (!StringUtils.hasText(dto.getAuditor())) {
            return Result.fail("审核人不能为空");
        }

        String DatasetName = dto.getDatasetName().trim();
        Integer status = dto.getStatus();
        String remark = dto.getRemark() != null ? dto.getRemark().trim() : null;
        String auditor = dto.getAuditor().trim();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatasetEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatasetEntity::getName, DatasetName)
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatasetEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验审核状态是否合法
        if (status != 0 && status != 1) {
            return Result.fail("审核状态必须为 0（驳回）或 1（通过）");
        }

        // 3. 更新审核信息
        LocalDateTime auditTime = LocalDateTime.now();

        LambdaUpdateWrapper<ManuDatasetEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ManuDatasetEntity::getName, DatasetName)
                .set(ManuDatasetEntity::getAuditStatus, status == 0 ? 2 : status); // 0 转为 2（驳回），1 保持（通过）
        int rows = DatasetDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("更新审核信息失败");
        }

        // 4. 返回审核结果
        AuditDatasetResultVO resultVO = new AuditDatasetResultVO();
        resultVO.setId(Dataset.getId());
        resultVO.setDatasetName(Dataset.getName());
        resultVO.setStatus(status == 0 ? 2 : status);
        resultVO.setRemark(remark);
        resultVO.setAuditor(auditor);
        resultVO.setAuditTime(auditTime);

        return Result.success(resultVO);
    }

    @Override
    public Result<Long> countDatasetsUnderMenu(Integer menuId) {
        if (menuId == null) {
            return Result.fail("目录id不能为空");
        }

        // menuId = 0 时，表示从根目录开始统计
        if (menuId != 0) {
            LambdaQueryWrapper<ManuDatasetEntity> menuWrapper = new LambdaQueryWrapper<>();
            menuWrapper.eq(ManuDatasetEntity::getId, menuId)
                    .eq(ManuDatasetEntity::getDeleted, 0)
                    .last("limit 1");

            ManuDatasetEntity currentMenu = DatasetDao.selectOne(menuWrapper);
            if (currentMenu == null) {
                return Result.fail("当前目录不存在");
            }
            if (currentMenu.getIsMenu() == null || currentMenu.getIsMenu() != 1) {
                return Result.fail("当前节点不是目录");
            }
        }

        // 一次性查出所有未删除的数据，内存递归统计
        LambdaQueryWrapper<ManuDatasetEntity> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(ManuDatasetEntity::getDeleted, 0);

        List<ManuDatasetEntity> allList = DatasetDao.selectList(allWrapper);

        Map<Integer, List<ManuDatasetEntity>> parentChildrenMap = new HashMap<>();
        for (ManuDatasetEntity item : allList) {
            Integer parentId = item.getParent() == null ? 0 : item.getParent();
            parentChildrenMap
                    .computeIfAbsent(parentId, k -> new ArrayList<>())
                    .add(item);
        }

        long count = countDatasetRecursively(menuId, parentChildrenMap);
        return Result.success(count);
    }

    @Override
    public Long getTotal() {
        return manuDatasetDao.selectCount(null);
    }

    private long countDatasetRecursively(Integer parentId, Map<Integer, List<ManuDatasetEntity>> parentChildrenMap) {
        List<ManuDatasetEntity> children = parentChildrenMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return 0L;
        }

        long count = 0L;
        for (ManuDatasetEntity child : children) {
            if (child.getIsMenu() != null && child.getIsMenu() == 0) {
                count++;
            } else if (child.getIsMenu() != null && child.getIsMenu() == 1) {
                count += countDatasetRecursively(child.getId(), parentChildrenMap);
            }
        }
        return count;
    }

}
