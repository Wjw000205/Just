package org.example.just.service.imp;

import org.example.just.dao.IndustryClassificationDao;
import org.example.just.dto.datasetDto.*;
import org.example.just.entity.IndustryClassificationEntity;
import org.example.just.service.SearchService;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImp implements SearchService {

    private final IndustryClassificationDao industryClassificationDao;

    public SearchServiceImp(IndustryClassificationDao industryClassificationDao) {
        this.industryClassificationDao = industryClassificationDao;
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
            // TODO: 这里需要根据实际业务逻辑实现数据集检索

            ArrayList<DatasetSearchResponse.DatasetItem> list = new ArrayList<>();
            DatasetSearchResponse response = new DatasetSearchResponse();
            response.setPage(request.getPage() != null ? request.getPage() : 1);
            response.setPageSize(request.getPageSize() != null ? request.getPageSize() : 10);
            response.setTotal(0);
            response.setItems(list);

            return Result.success("检索成功", response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("检索失败：" + e.getMessage());
        }
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
