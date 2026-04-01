package org.example.just.service;

import org.example.just.dto.datasetDto.*;
import org.example.just.utils.Result;

import java.util.List;

public interface SearchService {

    /**
     * 获取分类树
     */
    Result<List<CategoryTreeNode>> getCategories();

    /**
     * 数据集检索
     */
    Result<DatasetSearchResponse> searchDatasets(DatasetSearchRequest request);
}
