package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.just.dao.DatasetColumnDao;
import org.example.just.dao.DatasetDataDao;
import org.example.just.dao.IndustryClassificationDao;
import org.example.just.dao.ManuDatasetDao;
import org.example.just.dto.datasetDto.*;
import org.example.just.entity.DatasetColumnEntity;
import org.example.just.entity.IndustryClassificationEntity;
import org.example.just.entity.ManuDatasetEntity;
import org.example.just.service.SearchService;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImp implements SearchService {

    private final IndustryClassificationDao industryClassificationDao;
    private final ManuDatasetDao DatasetDao;
    private final DatasetColumnDao DatasetColumnDao;
    private final DatasetDataDao DatasetDataDao;

    public SearchServiceImp(IndustryClassificationDao industryClassificationDao,
                            ManuDatasetDao DatasetDao,
                            DatasetColumnDao DatasetColumnDao,
                            DatasetDataDao DatasetDataDao) {
        this.industryClassificationDao = industryClassificationDao;
        this.DatasetDao = DatasetDao;
        this.DatasetColumnDao = DatasetColumnDao;
        this.DatasetDataDao = DatasetDataDao;
    }

    @Override
    public Result<List<CategoryTreeNode>> getCategories() {
        try {
            // 查询所有分类
            List<IndustryClassificationEntity> allCategories = industryClassificationDao.selectAll();

            // 构建树形结构
            List<CategoryTreeNode> tree = buildCategoryTree(allCategories);

            return Result.success("获取分类树成功", tree);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取分类树失败：" + e.getMessage());
        }
    }



    @Override
    public Result<DatasetSearchResponse> searchDatasets(DatasetSearchRequest request) {
        try {
            if (request == null) {
                return Result.fail("请求参数不能为空");
            }

            Integer page = request.getPage() != null ? request.getPage() : 1;
            Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            String keyword = request.getKeyword();
            String searchField = request.getSearchField() != null ? request.getSearchField() : "fulltext";
            List<String> categoryIds = request.getCategoryIds() != null ? request.getCategoryIds() : new ArrayList<>();
            String resultType = request.getResultType();
            AdvancedSearchCondition advanced = request.getAdvanced();

            if (!StringUtils.hasText(resultType)) {
                return Result.fail("结果类型不能为空");
            }

            if (!"dataset".equals(resultType)) {
                DatasetSearchResponse response = new DatasetSearchResponse();
                response.setPage(page);
                response.setPageSize(pageSize);
                response.setTotal(0);
                response.setItems(new ArrayList<>());
                return Result.success("不支持的检索类型", response);
            }

            LambdaQueryWrapper<ManuDatasetEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ManuDatasetEntity::getDeleted, 0)
                    .eq(ManuDatasetEntity::getIsMenu, 0)
                    .eq(ManuDatasetEntity::getAuditStatus, 1);

            if (StringUtils.hasText(keyword)) {
                String trimmedKeyword = keyword.trim();
                switch (searchField) {
                    case "title":
                        wrapper.like(ManuDatasetEntity::getName, trimmedKeyword);
                        break;
                    case "keyword_field":
                        wrapper.like(ManuDatasetEntity::getName, trimmedKeyword);
                        break;
                    case "abstract":
                        wrapper.like(ManuDatasetEntity::getName, trimmedKeyword);
                        break;
                    case "paragraph_keyword":
                        wrapper.like(ManuDatasetEntity::getName, trimmedKeyword);
                        break;
                    case "fulltext":
                    default:
                        wrapper.like(ManuDatasetEntity::getName, trimmedKeyword);
                        break;
                }
            }

            if (advanced != null) {
                if (advanced.getConditions() != null && !advanced.getConditions().isEmpty()) {
                    for (AdvancedSearchCondition.ConditionItem condition : advanced.getConditions()) {
                        if (StringUtils.hasText(condition.getField()) && StringUtils.hasText(condition.getValue())) {
                            String value = condition.getValue().trim();
                            switch (condition.getField()) {
                                case "title":
                                    wrapper.like(ManuDatasetEntity::getName, value);
                                    break;
                                case "keyword_field":
                                    wrapper.like(ManuDatasetEntity::getName, value);
                                    break;
                                case "abstract":
                                    wrapper.like(ManuDatasetEntity::getName, value);
                                    break;
                                case "publisher":
                                    wrapper.like(ManuDatasetEntity::getCreator, value);
                                    break;
                                case "organization":
                                    wrapper.like(ManuDatasetEntity::getCreator, value);
                                    break;
                            }
                        }
                    }
                }

                if (StringUtils.hasText(advanced.getPublishDateStart()) ||
                        StringUtils.hasText(advanced.getPublishDateEnd())) {

                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                    if (StringUtils.hasText(advanced.getPublishDateStart())) {
                        try {
                            LocalDate startDate = LocalDate.parse(advanced.getPublishDateStart(), dateFormatter);
                            LocalDateTime startDateTime = startDate.atStartOfDay();
                            wrapper.ge(ManuDatasetEntity::getCreateTime, startDateTime);
                        } catch (Exception e) {
                        }
                    }

                    if (StringUtils.hasText(advanced.getPublishDateEnd())) {
                        try {
                            LocalDate endDate = LocalDate.parse(advanced.getPublishDateEnd(), dateFormatter);
                            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);                            wrapper.le(ManuDatasetEntity::getCreateTime, endDateTime);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            List<ManuDatasetEntity> datasetList = DatasetDao.selectList(wrapper);
            int total = datasetList.size();

            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            List<ManuDatasetEntity> pagedDatasetList = fromIndex < total
                    ? datasetList.subList(fromIndex, toIndex)
                    : new ArrayList<>();

            List<DatasetSearchResponse.DatasetItem> items = new ArrayList<>();
            for (ManuDatasetEntity dataset : pagedDatasetList) {
                DatasetSearchResponse.DatasetItem item = convertToDatasetItem(dataset, categoryIds);
                if (item != null) {
                    items.add(item);
                }
            }

            DatasetSearchResponse response = new DatasetSearchResponse();
            response.setPage(page);
            response.setPageSize(pageSize);
            response.setTotal(total);
            response.setItems(items);

            return Result.success("检索成功", response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("检索失败：" + e.getMessage());
        }
    }

    private DatasetSearchResponse.DatasetItem convertToDatasetItem(ManuDatasetEntity dataset, List<String> categoryIds) throws Exception {
        DatasetSearchResponse.DatasetItem item = new DatasetSearchResponse.DatasetItem();

        item.setId(String.valueOf(dataset.getId()));
        item.setTitle(dataset.getName());
        item.setCreator(dataset.getCreator());

        if (dataset.getCreateTime() != null) {
            item.setPublishTime(dataset.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, dataset.getName())
                .eq(DatasetColumnEntity::getDeleted, 0)
                .orderByAsc(DatasetColumnEntity::getId);

        List<DatasetColumnEntity> columns = DatasetColumnDao.selectList(columnWrapper);

        if (columns != null && !columns.isEmpty()) {
            List<String> columnNames = columns.stream()
                    .map(DatasetColumnEntity::getColumnName)
                    .collect(Collectors.toList());

            String abstractText = "包含字段：" + String.join(", ", columnNames);
            item.setAbstractText(abstractText.length() > 200 ? abstractText.substring(0, 200) + "..." : abstractText);
            item.setTags(columnNames);
        } else {
            item.setAbstractText("");
            item.setTags(new ArrayList<>());
        }

        List<DatasetSearchResponse.DatasetItem.CategoryPathItem> categoryPath = new ArrayList<>();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (String categoryId : categoryIds) {
                IndustryClassificationEntity category = industryClassificationDao.selectById(categoryId);
                if (category != null) {
                    DatasetSearchResponse.DatasetItem.CategoryPathItem pathItem =
                            new DatasetSearchResponse.DatasetItem.CategoryPathItem();
                    pathItem.setId(categoryId);
                    pathItem.setName(category.getName());
                    categoryPath.add(pathItem);
                }
            }
        }
        item.setCategoryPath(categoryPath);

        item.setDownloadCount(0);
        item.setViewCount(0);
        item.setHasFullText(true);
        item.setOrganization("");
        item.setExtra(null);

        return item;
    }

    /**
     * 构建分类树
     */
    private List<CategoryTreeNode> buildCategoryTree(List<IndustryClassificationEntity> allCategories) {
        // 按层级和排序字段排序
        allCategories.sort(Comparator.comparing(IndustryClassificationEntity::getLevel)
                .thenComparing(IndustryClassificationEntity::getSortOrder));

        // 转换为 Map 便于查找
        Map<String, CategoryTreeNode> nodeMap = allCategories.stream()
                .collect(Collectors.toMap(
                        IndustryClassificationEntity::getId,
                        entity -> {
                            CategoryTreeNode node = new CategoryTreeNode();
                            node.setId(entity.getId());
                            node.setName(entity.getName());
                            node.setChildren(new ArrayList<>());
                            return node;
                        }
                ));

        // 构建树形结构
        List<CategoryTreeNode> rootNodes = new ArrayList<>();
        for (IndustryClassificationEntity entity : allCategories) {
            CategoryTreeNode node = nodeMap.get(entity.getId());
            String parentId = entity.getParentId();

            if (parentId == null || parentId.isEmpty()) {
                // 顶级节点
                rootNodes.add(node);
            } else {
                // 子节点，添加到父节点
                CategoryTreeNode parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            }
        }

        return rootNodes;
    }
}
