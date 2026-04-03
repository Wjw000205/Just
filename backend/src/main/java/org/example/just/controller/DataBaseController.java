package org.example.just.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.databaseDto.*;
import org.example.just.service.DataBaseService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/database")
@Tag(name = "数据库操作接口", description = "数据库相关操作")
public class DataBaseController {

    @Resource
    private DataBaseService dataBaseService;
    
    @GetMapping("/page/init")
    @Operation(summary = "获取页面初始化信息", description = "获取数据库页面的初始化信息")
    public Result<DataBasePageInitInfoVO> getPageInitInfo() {
        return dataBaseService.getPageInitInfo();
    }

    /**
     * tag: DataBaseController
     * operation: queryDatasets
     */
    @PostMapping("/datasets/query")
    @Operation(summary = "查询数据集列表", description = "数据库页面主表格数据查询")
    public Result<DatasetQueryResponse> queryDatasets(@RequestBody DatasetQueryRequest request) {
        return dataBaseService.queryDatasets(request);
    }

    /**
     * tag: DataBaseController
     * operation: getDatasetDetail
     */
    @GetMapping("/datasets/{id}")
    @Operation(summary = "获取数据集详情", description = "详情页顶部信息卡")
    public Result<DatasetDetailVO> getDatasetDetail(@PathVariable Integer id) {
        return dataBaseService.getDatasetDetail(id);
    }

    /**
     * tag: DataBaseController
     * operation: queryDatasetRecords
     */
    @PostMapping("/datasets/{id}/records/query")
    @Operation(summary = "查询数据集记录", description = "详情页右侧表格（文献/实验等）")
    public Result<DatasetRecordQueryResponse> queryDatasetRecords(
            @PathVariable Integer id,
            @RequestBody DatasetRecordQueryRequest request) {
        return dataBaseService.queryDatasetRecords(id, request);
    }

    /**
     * tag: DataBaseController
     * operation: favoriteDataset
     */
    @PostMapping("/datasets/{id}/favorite")
    @Operation(summary = "收藏/取消收藏数据集", description = "收藏或取消收藏指定数据集")
    public Result<FavoriteResponse> favoriteDataset(
            @PathVariable Integer id,
            @RequestBody FavoriteRequest request) {
        return dataBaseService.favoriteDataset(id, request);
    }

    /**
     * tag: DataBaseController
     * operation: downloadDataset
     */
    @PostMapping("/datasets/{id}/download")
    @Operation(summary = "下载数据集", description = "下载全字段或部分字段数据")
    public void downloadDataset(
            @PathVariable Long id,
            @RequestBody DownloadRequest request,
            HttpServletResponse response) {
        dataBaseService.downloadDataset(id, request, response);
    }

    /**
     * tag: DataBaseController
     * operation: getDownloadFields
     */
    @GetMapping("/datasets/{id}/download-fields")
    @Operation(summary = "获取可下载字段列表", description = "获取部分字段下载时的可选字段列表")
    public Result<DownloadFieldVO> getDownloadFields(@PathVariable Long id) {
        return dataBaseService.getDownloadFields(id);
    }
}
