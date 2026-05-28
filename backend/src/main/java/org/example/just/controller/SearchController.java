package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.just.dto.datasetDto.DatasetSearchRequest;
import org.example.just.dto.datasetDto.DatasetSearchResponse;
import org.example.just.service.SearchService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "搜索操作接口", description = "Search 相关操作")
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/categories")
    @Operation(summary = "获取分类树", description = "用于获取左侧'工业战略性新兴产业分类目录'树形数据")
    public Result<List<org.example.just.dto.datasetDto.CategoryTreeNode>> getCategories() {
        return searchService.getCategories();
    }

    @PostMapping("/datasets")
    @Operation(summary = "数据集检索", description = "对数据集进行全文检索，并支持按分类、结果类型、分页等过滤。高级检索也复用本接口")
    public Result<DatasetSearchResponse> searchDatasets(@RequestBody DatasetSearchRequest request) {
        return searchService.searchDatasets(request);
    }
}
