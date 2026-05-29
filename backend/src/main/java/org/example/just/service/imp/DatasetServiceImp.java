package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatasetServiceImp implements DatasetService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                    vo.setAuditStatus(dataset.getAuditStatus());
                    vo.setRecordCount(countDatasetRecords(dataset.getName()));
                    return vo;
                })
                .collect(Collectors.toList());

        return DatasetOptionsResult.success(options, total);
    }

    @Override
    public Result<List<DatasetTagVO>> getDatasetTags() {
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDeleted, 0)
                .orderByAsc(DatasetColumnEntity::getId);
        List<DatasetColumnEntity> columns = DatasetColumnDao.selectList(columnWrapper);
        if (columns == null || columns.isEmpty()) {
            return Result.success(0, "success", new ArrayList<>());
        }

        Map<String, DatasetTagVO> tagMap = new LinkedHashMap<>();
        for (DatasetColumnEntity column : columns) {
            if (column == null
                    || (column.getDeleted() != null && column.getDeleted() == 1)
                    || !StringUtils.hasText(column.getColumnName())) {
                continue;
            }
            String tagName = column.getColumnName().trim();
            DatasetTagVO existing = tagMap.get(tagName);
            if (existing == null) {
                tagMap.put(tagName, new DatasetTagVO(column.getId(), tagName));
            } else if (column.getId() != null
                    && (existing.getId() == null || column.getId() < existing.getId())) {
                existing.setId(column.getId());
            }
        }

        return Result.success(0, "success", new ArrayList<>(tagMap.values()));
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

        List<DatasetColumnEntity> datasetColumns = queryDatasetColumns(dataset.getName());
        List<OnlineFormSectionVO> sections = new ArrayList<>();
        if (dataset.getModule() != null) {
            LambdaQueryWrapper<ModuleColumnEntity> templateColumnWrapper = new LambdaQueryWrapper<>();
            templateColumnWrapper.eq(ModuleColumnEntity::getModuleId, dataset.getModule())
                    .eq(ModuleColumnEntity::getDeleted, 0)
                    .orderByAsc(ModuleColumnEntity::getCreateTime)
                    .orderByAsc(ModuleColumnEntity::getId);
            List<ModuleColumnEntity> templateColumns = moduleColumnDao.selectList(templateColumnWrapper);
            sections = buildTemplateSections(datasetColumns, templateColumns);
        }

        if (sections.isEmpty()) {
            sections = buildDatasetColumnSections(dataset, datasetColumns);
        }

        OnlineFormSchemaVO schema = new OnlineFormSchemaVO();
        schema.setSections(sections);
        return Result.success(0, "success", schema);
    }

    private List<OnlineFormSectionVO> buildTemplateSections(List<DatasetColumnEntity> datasetColumns,
                                                            List<ModuleColumnEntity> templateColumns) {
        if (datasetColumns == null || datasetColumns.isEmpty() || templateColumns == null || templateColumns.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, String> belongByColumnId = matchDatasetColumnBelongs(datasetColumns, templateColumns);
        Map<String, List<OnlineFormFieldVO>> fieldsByBelong = new LinkedHashMap<>();
        for (int i = 0; i < datasetColumns.size(); i++) {
            DatasetColumnEntity column = datasetColumns.get(i);
            String belong = normalizeTemplateBelong(belongByColumnId.get(column.getId()));
            fieldsByBelong
                    .computeIfAbsent(belong, key -> new ArrayList<>())
                    .add(buildOnlineFormField(column, i));
        }

        List<OnlineFormSectionVO> sections = new ArrayList<>();
        addTemplateSection(sections, fieldsByBelong.remove("Object"), "Object");
        addTemplateSection(sections, fieldsByBelong.remove("Operation"), "Operation");
        addTemplateSection(sections, fieldsByBelong.remove("Result"), "Result");

        for (Map.Entry<String, List<OnlineFormFieldVO>> entry : fieldsByBelong.entrySet()) {
            addTemplateSection(sections, entry.getValue(), entry.getKey());
        }
        return sections;
    }

    private Map<Integer, String> matchDatasetColumnBelongs(List<DatasetColumnEntity> datasetColumns,
                                                           List<ModuleColumnEntity> templateColumns) {
        Map<String, Deque<ModuleColumnEntity>> templateColumnsByName = new LinkedHashMap<>();
        for (ModuleColumnEntity templateColumn : templateColumns) {
            if (!StringUtils.hasText(templateColumn.getColumnName())) {
                continue;
            }
            templateColumnsByName
                    .computeIfAbsent(templateColumn.getColumnName().trim(), key -> new ArrayDeque<>())
                    .add(templateColumn);
        }

        Map<Integer, String> belongByColumnId = new LinkedHashMap<>();
        for (int i = 0; i < datasetColumns.size(); i++) {
            DatasetColumnEntity datasetColumn = datasetColumns.get(i);
            ModuleColumnEntity matchedTemplateColumn = null;
            if (StringUtils.hasText(datasetColumn.getColumnName())) {
                Deque<ModuleColumnEntity> sameNameColumns = templateColumnsByName.get(datasetColumn.getColumnName().trim());
                if (sameNameColumns != null && !sameNameColumns.isEmpty()) {
                    matchedTemplateColumn = sameNameColumns.pollFirst();
                }
            }
            if (matchedTemplateColumn == null && i < templateColumns.size()) {
                matchedTemplateColumn = templateColumns.get(i);
            }
            if (matchedTemplateColumn != null && datasetColumn.getId() != null) {
                belongByColumnId.put(datasetColumn.getId(), matchedTemplateColumn.getBelong());
            }
        }
        return belongByColumnId;
    }

    private List<OnlineFormSectionVO> buildDatasetColumnSections(ManuDatasetEntity dataset,
                                                                 List<DatasetColumnEntity> columns) {
        if (columns == null) {
            columns = new ArrayList<>();
        }

        OnlineFormSectionVO section = new OnlineFormSectionVO();
        section.setId(dataset.getId() == null ? "dataset" : "dataset" + dataset.getId());
        section.setTitle(StringUtils.hasText(dataset.getName()) ? dataset.getName() : "数据集字段");
        section.setSubtitle("");

        List<OnlineFormFieldVO> fields = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            fields.add(buildOnlineFormField(columns.get(i), i));
        }
        section.setFields(fields);

        return List.of(section);
    }

    private List<DatasetColumnEntity> queryDatasetColumns(String datasetName) {
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, datasetName)
                .eq(DatasetColumnEntity::getDeleted, 0)
                .orderByAsc(DatasetColumnEntity::getId);
        List<DatasetColumnEntity> columns = DatasetColumnDao.selectList(columnWrapper);
        if (columns == null) {
            return new ArrayList<>();
        }
        return columns;
    }

    @Transactional
    @Override
    public Result<OnlineFormSubmitResultVO> submitOnlineFormData(OnlineFormSubmitDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getDatasetId() == null) {
            return Result.fail("数据集ID不能为空");
        }
        if (dto.getRecords() == null || dto.getRecords().isEmpty()) {
            return Result.fail("记录数组不能为空");
        }

        LambdaQueryWrapper<ManuDatasetEntity> datasetWrapper = new LambdaQueryWrapper<>();
        datasetWrapper.eq(ManuDatasetEntity::getId, dto.getDatasetId())
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .eq(ManuDatasetEntity::getDeleted, 0)
                .last("limit 1");
        ManuDatasetEntity dataset = DatasetDao.selectOne(datasetWrapper);
        if (dataset == null) {
            return Result.fail("数据集不存在");
        }

        List<DatasetColumnEntity> columns = queryDatasetColumns(dataset.getName());
        if (columns.isEmpty()) {
            return Result.fail("当前数据集未定义列");
        }

        List<Integer> columnIds = columns.stream()
                .map(DatasetColumnEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (columnIds.isEmpty()) {
            return Result.fail("当前数据集未定义有效列");
        }

        Map<String, DatasetColumnEntity> columnsByField = buildOnlineSubmitColumnIndex(columns);
        Integer maxRowId = DatasetDataDao.selectMaxRowIdByColumnIds(columnIds);
        int nextRowId = (maxRowId == null ? 0 : maxRowId) + 1;

        OnlineFormSubmitResultVO resultVO = new OnlineFormSubmitResultVO();
        List<OnlineFormSubmitErrorVO> errors = new ArrayList<>();
        List<DatasetDataEntity> batchList = new ArrayList<>();
        int acceptedCount = 0;
        int failedCount = 0;

        for (int rowIndex = 0; rowIndex < dto.getRecords().size(); rowIndex++) {
            Map<String, Object> record = dto.getRecords().get(rowIndex);
            List<OnlineFormSubmitErrorVO> rowErrors = validateOnlineSubmitRecord(rowIndex, record, columns, columnsByField);
            if (!rowErrors.isEmpty()) {
                failedCount++;
                errors.addAll(rowErrors);
                continue;
            }

            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                DatasetColumnEntity column = columns.get(columnIndex);
                Object value = getOnlineSubmitValue(record, column, columnIndex);

                DatasetDataEntity data = new DatasetDataEntity();
                data.setColumnId(column.getId());
                data.setRowId(nextRowId);
                data.setDataType(column.getColumnType());
                data.setData(toOnlineSubmitData(value));
                data.setDeleted(0);
                batchList.add(data);
            }
            acceptedCount++;
            nextRowId++;
        }

        if (!batchList.isEmpty()) {
            int rows = DatasetDataDao.insertBatch(batchList);
            if (rows <= 0) {
                throw new RuntimeException("在线填写数据保存失败");
            }
        }

        resultVO.setAcceptedCount(acceptedCount);
        resultVO.setFailedCount(failedCount);
        resultVO.setErrors(errors);

        String message = "success";
        if (failedCount > 0) {
            message = acceptedCount > 0 ? "partially_success" : "failed";
        }
        return Result.success(0, message, resultVO);
    }

    private Map<String, DatasetColumnEntity> buildOnlineSubmitColumnIndex(List<DatasetColumnEntity> columns) {
        Map<String, DatasetColumnEntity> columnsByField = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            DatasetColumnEntity column = columns.get(i);
            columnsByField.put(getOnlineFormFieldId(column, i), column);
            if (StringUtils.hasText(column.getColumnName())) {
                columnsByField.putIfAbsent(column.getColumnName().trim(), column);
            }
        }
        return columnsByField;
    }

    private List<OnlineFormSubmitErrorVO> validateOnlineSubmitRecord(Integer rowIndex,
                                                                     Map<String, Object> record,
                                                                     List<DatasetColumnEntity> columns,
                                                                     Map<String, DatasetColumnEntity> columnsByField) {
        List<OnlineFormSubmitErrorVO> errors = new ArrayList<>();
        if (record == null || record.isEmpty()) {
            errors.add(new OnlineFormSubmitErrorVO(rowIndex, "", "记录不能为空"));
            return errors;
        }

        for (String field : record.keySet()) {
            if (!columnsByField.containsKey(field)) {
                errors.add(new OnlineFormSubmitErrorVO(rowIndex, field, "字段不存在"));
            }
        }

        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            DatasetColumnEntity column = columns.get(columnIndex);
            String field = getOnlineFormFieldId(column, columnIndex);
            String value = toOnlineSubmitData(getOnlineSubmitValue(record, column, columnIndex));
            if (!StringUtils.hasText(value)) {
                errors.add(new OnlineFormSubmitErrorVO(rowIndex, field, column.getColumnName() + "不能为空"));
            }
        }
        return errors;
    }

    private Object getOnlineSubmitValue(Map<String, Object> record, DatasetColumnEntity column, int columnIndex) {
        String field = getOnlineFormFieldId(column, columnIndex);
        if (record.containsKey(field)) {
            return record.get(field);
        }
        if (StringUtils.hasText(column.getColumnName()) && record.containsKey(column.getColumnName().trim())) {
            return record.get(column.getColumnName().trim());
        }
        return null;
    }

    private String toOnlineSubmitData(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text.trim();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private void addTemplateSection(List<OnlineFormSectionVO> sections, List<OnlineFormFieldVO> fields, String belong) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        OnlineFormSectionVO section = new OnlineFormSectionVO();
        section.setId(toOnlineFormSectionId(belong));
        section.setTitle(toOnlineFormSectionTitle(belong));
        section.setSubtitle("");
        section.setFields(fields);
        sections.add(section);
    }

    private OnlineFormFieldVO buildOnlineFormField(DatasetColumnEntity column, int index) {
        OnlineFormFieldVO field = new OnlineFormFieldVO();
        field.setId(getOnlineFormFieldId(column, index));
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

    private String getOnlineFormFieldId(DatasetColumnEntity column, int index) {
        return column.getId() == null ? "field" + (index + 1) : "column" + column.getId();
    }

    private String normalizeTemplateBelong(String belong) {
        if (!StringUtils.hasText(belong)) {
            return "Dataset";
        }
        String normalizedBelong = belong.trim();
        if ("Object".equalsIgnoreCase(normalizedBelong)) {
            return "Object";
        }
        if ("Operation".equalsIgnoreCase(normalizedBelong)) {
            return "Operation";
        }
        if ("Result".equalsIgnoreCase(normalizedBelong)) {
            return "Result";
        }
        return normalizedBelong;
    }

    private String toOnlineFormSectionId(String belong) {
        String normalizedBelong = normalizeTemplateBelong(belong);
        if ("Object".equals(normalizedBelong)) {
            return "object";
        }
        if ("Operation".equals(normalizedBelong)) {
            return "operation";
        }
        if ("Result".equals(normalizedBelong)) {
            return "result";
        }
        return normalizedBelong.trim().toLowerCase(Locale.ROOT);
    }

    private String toOnlineFormSectionTitle(String belong) {
        String normalizedBelong = normalizeTemplateBelong(belong);
        if ("Object".equals(normalizedBelong)) {
            return "对象区域";
        }
        if ("Operation".equals(normalizedBelong)) {
            return "操作区域";
        }
        if ("Result".equals(normalizedBelong)) {
            return "结果区域";
        }
        return normalizedBelong;
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
    public Result<BatchUploadResultVO> importDatasetData(Integer datasetId, MultipartFile file) {
        if (datasetId == null) {
            return Result.fail("数据集ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        LambdaQueryWrapper<ManuDatasetEntity> datasetWrapper = new LambdaQueryWrapper<>();
        datasetWrapper.eq(ManuDatasetEntity::getId, datasetId)
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .eq(ManuDatasetEntity::getDeleted, 0)
                .last("limit 1");
        ManuDatasetEntity dataset = DatasetDao.selectOne(datasetWrapper);
        if (dataset == null) {
            return Result.fail("数据集不存在");
        }

        List<DatasetColumnEntity> columns = queryDatasetColumns(dataset.getName());
        if (columns.isEmpty()) {
            return Result.fail("当前数据集未定义列");
        }

        List<Integer> columnIds = columns.stream()
                .map(DatasetColumnEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (columnIds.isEmpty()) {
            return Result.fail("当前数据集未定义有效列");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Result.fail("Excel中没有可读取的Sheet");
            }

            Row groupHeaderRow = sheet.getRow(0);
            Row fieldHeaderRow = sheet.getRow(1);
            if (groupHeaderRow == null || fieldHeaderRow == null) {
                return Result.fail("Excel表头必须包含分组行和字段行");
            }

            List<TemplateColumnGroup> templateGroups = buildTemplateColumnGroups(dataset, columns);
            Map<Integer, DatasetColumnEntity> matchedColumnIndexMap = new LinkedHashMap<>();
            DataFormatter dataFormatter = new DataFormatter();
            int lastCellNum = Math.max(groupHeaderRow.getLastCellNum(), fieldHeaderRow.getLastCellNum());
            String lastGroupName = null;
            for (int i = 0; i < lastCellNum; i++) {
                String groupName = getHeaderCellValue(sheet, groupHeaderRow, i, dataFormatter);
                if (StringUtils.hasText(groupName)) {
                    lastGroupName = groupName;
                } else {
                    groupName = lastGroupName;
                }

                String fieldName = dataFormatter.formatCellValue(fieldHeaderRow.getCell(i)).trim();
                if (!StringUtils.hasText(fieldName)) {
                    continue;
                }

                DatasetColumnEntity matchedColumn = findBatchUploadColumn(templateGroups, groupName, fieldName);
                if (matchedColumn != null) {
                    matchedColumnIndexMap.put(i, matchedColumn);
                }
            }

            if (matchedColumnIndexMap.isEmpty()) {
                return Result.fail("Excel表头与数据集字段没有任何匹配项");
            }

            Integer maxRowId = DatasetDataDao.selectMaxRowIdByColumnIds(columnIds);
            int nextRowId = (maxRowId == null ? 0 : maxRowId) + 1;
            int firstMatchedColumnIndex = matchedColumnIndexMap.keySet().iterator().next();
            DatasetColumnEntity rowKeyColumn = matchedColumnIndexMap.get(firstMatchedColumnIndex);
            Set<String> rowKeys = new HashSet<>();
            BatchUploadResultVO resultVO = new BatchUploadResultVO();
            resultVO.setTaskId(createBatchUploadTaskId());
            List<BatchUploadErrorVO> errors = new ArrayList<>();
            List<DatasetDataEntity> batchList = new ArrayList<>();
            int lastRowNum = sheet.getLastRowNum();
            int acceptedCount = 0;
            int failedCount = 0;

            for (int rowIndex = 2; rowIndex <= lastRowNum; rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null) {
                    continue;
                }

                boolean rowHasValue = false;
                List<DatasetDataEntity> currentRowDataList = new ArrayList<>();
                for (Map.Entry<Integer, DatasetColumnEntity> entry : matchedColumnIndexMap.entrySet()) {
                    Integer cellIndex = entry.getKey();
                    DatasetColumnEntity datasetColumn = entry.getValue();

                    String cellValue = dataFormatter.formatCellValue(dataRow.getCell(cellIndex)).trim();

                    if (StringUtils.hasText(cellValue)) {
                        rowHasValue = true;
                    }

                    DatasetDataEntity datasetData = new DatasetDataEntity();
                    datasetData.setColumnId(datasetColumn.getId());
                    datasetData.setRowId(nextRowId);
                    datasetData.setDataType(datasetColumn.getColumnType());
                    datasetData.setData(cellValue);
                    datasetData.setDeleted(0);

                    currentRowDataList.add(datasetData);
                }

                if (!rowHasValue) {
                    continue;
                }

                int excelRowIndex = rowIndex + 1;
                String rowKey = dataFormatter.formatCellValue(dataRow.getCell(firstMatchedColumnIndex)).trim();
                if (!StringUtils.hasText(rowKey)) {
                    failedCount++;
                    errors.add(new BatchUploadErrorVO(excelRowIndex, rowKeyColumn.getColumnName() + "不能为空"));
                    continue;
                }
                if (!rowKeys.add(rowKey)) {
                    failedCount++;
                    errors.add(new BatchUploadErrorVO(excelRowIndex, rowKeyColumn.getColumnName() + "重复"));
                    continue;
                }

                batchList.addAll(currentRowDataList);
                acceptedCount++;
                nextRowId++;
            }

            if (!batchList.isEmpty()) {
                int rows = DatasetDataDao.insertBatch(batchList);
                if (rows <= 0) {
                    throw new RuntimeException("批量上传数据保存失败");
                }
            }

            resultVO.setAcceptedCount(acceptedCount);
            resultVO.setFailedCount(failedCount);
            resultVO.setErrors(errors);
            return Result.success(0, "success", resultVO);

        } catch (Exception e) {
            throw new RuntimeException("批量上传数据失败: " + e.getMessage(), e);
        }
    }

    private String getHeaderCellValue(Sheet sheet, Row row, int columnIndex, DataFormatter dataFormatter) {
        String value = dataFormatter.formatCellValue(row.getCell(columnIndex)).trim();
        if (StringUtils.hasText(value)) {
            return value;
        }

        for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
            if (!mergedRegion.isInRange(row.getRowNum(), columnIndex)) {
                continue;
            }
            Row firstRow = sheet.getRow(mergedRegion.getFirstRow());
            if (firstRow == null) {
                return "";
            }
            return dataFormatter.formatCellValue(firstRow.getCell(mergedRegion.getFirstColumn())).trim();
        }
        return "";
    }

    private DatasetColumnEntity findBatchUploadColumn(List<TemplateColumnGroup> templateGroups,
                                                      String groupName,
                                                      String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return null;
        }

        String normalizedGroupName = normalizeBatchHeader(groupName);
        String normalizedFieldName = fieldName.trim();
        DatasetColumnEntity fieldOnlyMatch = null;
        int fieldOnlyMatchCount = 0;

        for (TemplateColumnGroup group : templateGroups) {
            boolean groupMatched = normalizeBatchHeader(group.getName()).equals(normalizedGroupName);
            for (DatasetColumnEntity column : group.getColumns()) {
                if (!StringUtils.hasText(column.getColumnName())
                        || !column.getColumnName().trim().equals(normalizedFieldName)) {
                    continue;
                }
                if (groupMatched) {
                    return column;
                }
                fieldOnlyMatch = column;
                fieldOnlyMatchCount++;
            }
        }

        return fieldOnlyMatchCount == 1 ? fieldOnlyMatch : null;
    }

    private String normalizeBatchHeader(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String createBatchUploadTaskId() {
        return "job-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
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
    public void exportDatasetTemplate(Integer datasetId, String format, HttpServletResponse response) {
        if (datasetId == null) {
            writeTemplateError(response, 400, "数据集ID不能为空");
            return;
        }
        if (!StringUtils.hasText(format)) {
            writeTemplateError(response, 400, "模板格式不能为空");
            return;
        }

        String normalizedFormat = format.trim().toLowerCase(Locale.ROOT);
        if ("xlsx".equals(normalizedFormat)) {
            normalizedFormat = "excel";
        }
        if (!"json".equals(normalizedFormat) && !"excel".equals(normalizedFormat)) {
            writeTemplateError(response, 400, "模板格式只支持 json 或 excel");
            return;
        }

        LambdaQueryWrapper<ManuDatasetEntity> datasetWrapper = new LambdaQueryWrapper<>();
        datasetWrapper.eq(ManuDatasetEntity::getId, datasetId)
                .eq(ManuDatasetEntity::getIsMenu, 0)
                .eq(ManuDatasetEntity::getDeleted, 0)
                .last("limit 1");
        ManuDatasetEntity dataset = DatasetDao.selectOne(datasetWrapper);
        if (dataset == null) {
            writeTemplateError(response, 404, "数据集不存在");
            return;
        }

        List<DatasetColumnEntity> columns = queryDatasetColumns(dataset.getName());
        if (columns.isEmpty()) {
            writeTemplateError(response, 400, "当前数据集未定义列，无法导出模板");
            return;
        }

        List<TemplateColumnGroup> groups = buildTemplateColumnGroups(dataset, columns);
        try {
            if ("json".equals(normalizedFormat)) {
                writeJsonTemplate(dataset, groups, response);
            } else {
                writeExcelTemplate(dataset, groups, response);
            }
        } catch (Exception e) {
            throw new RuntimeException("导出模板失败: " + e.getMessage(), e);
        }
    }

    private List<TemplateColumnGroup> buildTemplateColumnGroups(ManuDatasetEntity dataset,
                                                                List<DatasetColumnEntity> columns) {
        Map<Integer, String> belongByColumnId = new LinkedHashMap<>();
        if (dataset.getModule() != null) {
            LambdaQueryWrapper<ModuleColumnEntity> templateColumnWrapper = new LambdaQueryWrapper<>();
            templateColumnWrapper.eq(ModuleColumnEntity::getModuleId, dataset.getModule())
                    .eq(ModuleColumnEntity::getDeleted, 0)
                    .orderByAsc(ModuleColumnEntity::getCreateTime)
                    .orderByAsc(ModuleColumnEntity::getId);
            List<ModuleColumnEntity> templateColumns = moduleColumnDao.selectList(templateColumnWrapper);
            if (templateColumns != null && !templateColumns.isEmpty()) {
                belongByColumnId = matchDatasetColumnBelongs(columns, templateColumns);
            }
        }

        Map<String, TemplateColumnGroup> groupMap = new LinkedHashMap<>();
        for (DatasetColumnEntity column : columns) {
            String belong = normalizeTemplateBelong(belongByColumnId.get(column.getId()));
            TemplateColumnGroup group = groupMap.computeIfAbsent(belong, key ->
                    new TemplateColumnGroup(toOnlineFormSectionId(key), new ArrayList<>()));
            group.getColumns().add(column);
        }

        List<TemplateColumnGroup> result = new ArrayList<>();
        addTemplateColumnGroup(result, groupMap.remove("Object"));
        addTemplateColumnGroup(result, groupMap.remove("Operation"));
        addTemplateColumnGroup(result, groupMap.remove("Result"));
        for (TemplateColumnGroup group : groupMap.values()) {
            addTemplateColumnGroup(result, group);
        }
        return result;
    }

    private void addTemplateColumnGroup(List<TemplateColumnGroup> result, TemplateColumnGroup group) {
        if (group != null && !group.getColumns().isEmpty()) {
            result.add(group);
        }
    }

    private void writeExcelTemplate(ManuDatasetEntity dataset,
                                    List<TemplateColumnGroup> groups,
                                    HttpServletResponse response) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("template");
            Row groupRow = sheet.createRow(0);
            Row fieldRow = sheet.createRow(1);

            CellStyle groupStyle = workbook.createCellStyle();
            groupStyle.setAlignment(HorizontalAlignment.CENTER);
            groupStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            int columnIndex = 0;
            for (TemplateColumnGroup group : groups) {
                int startIndex = columnIndex;
                Cell groupCell = groupRow.createCell(startIndex);
                groupCell.setCellValue(group.getName());
                groupCell.setCellStyle(groupStyle);

                for (DatasetColumnEntity column : group.getColumns()) {
                    Cell fieldCell = fieldRow.createCell(columnIndex);
                    fieldCell.setCellValue(column.getColumnName());
                    columnIndex++;
                }

                if (columnIndex - startIndex > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, startIndex, columnIndex - 1));
                }
            }

            for (int i = 0; i < columnIndex; i++) {
                sheet.autoSizeColumn(i);
            }

            setTemplateDownloadHeaders(response, dataset.getName() + "_template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();
        }
    }

    private void writeJsonTemplate(ManuDatasetEntity dataset,
                                   List<TemplateColumnGroup> groups,
                                   HttpServletResponse response) throws Exception {
        List<String> groupHeader = new ArrayList<>();
        List<String> fieldHeader = new ArrayList<>();
        List<Map<String, Object>> sections = new ArrayList<>();

        for (TemplateColumnGroup group : groups) {
            List<String> fields = new ArrayList<>();
            for (int i = 0; i < group.getColumns().size(); i++) {
                DatasetColumnEntity column = group.getColumns().get(i);
                groupHeader.add(i == 0 ? group.getName() : "");
                fieldHeader.add(column.getColumnName());
                fields.add(column.getColumnName());
            }

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("name", group.getName());
            section.put("fields", fields);
            sections.add(section);
        }

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("datasetId", dataset.getId());
        template.put("datasetName", dataset.getName());
        template.put("headers", List.of(groupHeader, fieldHeader));
        template.put("sections", sections);

        setTemplateDownloadHeaders(response, dataset.getName() + "_template.json", "application/json;charset=UTF-8");
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), template);
        response.getOutputStream().flush();
    }

    private void setTemplateDownloadHeaders(HttpServletResponse response, String fileName, String contentType) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setContentType(contentType);
        if (contentType.startsWith("application/json")) {
            response.setCharacterEncoding("UTF-8");
        }
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
    }

    private void writeTemplateError(HttpServletResponse response, Integer code, String message) {
        try {
            response.setStatus(code);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            OBJECT_MAPPER.writeValue(response.getOutputStream(), Result.fail(code, message));
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException("写入模板错误响应失败: " + e.getMessage(), e);
        }
    }

    private static class TemplateColumnGroup {
        private final String name;
        private final List<DatasetColumnEntity> columns;

        private TemplateColumnGroup(String name, List<DatasetColumnEntity> columns) {
            this.name = name;
            this.columns = columns;
        }

        private String getName() {
            return name;
        }

        private List<DatasetColumnEntity> getColumns() {
            return columns;
        }
    }

    @Transactional
    @Override
    public Result<String> addDatasetColumn(AddDatasetColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("数据集名称不能为空");
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
        return DatasetDao.selectCount(null);
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
