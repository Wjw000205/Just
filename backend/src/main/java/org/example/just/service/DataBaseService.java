package org.example.just.service;

import org.example.just.dto.databaseDto.*;
import org.example.just.utils.Result;

public interface DataBaseService {
    Result<DataBasePageInitInfoVO> getPageInitInfo();
    
    /**
     * tag: DataBaseService
     * operation: queryDatasets
     * 查询数据集列表
     */
    Result<DatasetQueryResponse> queryDatasets(DatasetQueryRequest request);
    
    /**
     * tag: DataBaseService
     * operation: getDatasetDetail
     * 获取数据集详情
     */
    Result<DatasetDetailVO> getDatasetDetail(Integer id);
    
    /**
     * tag: DataBaseService
     * operation: queryDatasetRecords
     * 查询数据集记录
     */
    Result<DatasetRecordQueryResponse> queryDatasetRecords(Integer id, DatasetRecordQueryRequest request);
    
    /**
     * tag: DataBaseService
     * operation: favoriteDataset
     * 收藏/取消收藏数据集
     */
    Result<FavoriteResponse> favoriteDataset(Integer id, FavoriteRequest request);
    
    /**
     * tag: DataBaseService
     * operation: downloadDataset
     * 下载数据集
     */
    void downloadDataset(Long id, DownloadRequest request, jakarta.servlet.http.HttpServletResponse response);
    
    /**
     * tag: DataBaseService
     * operation: getDownloadFields
     * 获取可下载字段列表
     */
    Result<DownloadFieldVO> getDownloadFields(Long id);
}
