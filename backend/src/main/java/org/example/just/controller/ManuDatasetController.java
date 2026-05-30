package org.example.just.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.categoryDto.ProductCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ProductCategoryTreeResult;
import org.example.just.dto.categoryDto.ScienceCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeResult;
import org.example.just.dto.datasetDto.*;
import org.example.just.service.DatasetService;
import org.example.just.service.SearchService;
import org.example.just.utils.PageQuery;
import org.example.just.utils.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "目录数据库操作接口", description = "模板目录相关操作")
public class ManuDatasetController {

    private final DatasetService datasetService;
    private final SearchService searchService;

    public ManuDatasetController(DatasetService datasetService, SearchService searchService) {
        this.datasetService = datasetService;
        this.searchService = searchService;
    }

    @PostMapping("/Dataset/create-menu")
    @Operation(summary = "创建目录", description = "仅向 Dataset 表插入一条目录记录")
    public Result<String> createMenu(@RequestBody CreateMenuDatasetDTO dto) {
        return datasetService.createMenu(dto);
    }

    @PostMapping("/api/categories/science/tree")
    @Operation(summary = "科学分类树", description = "科学分类弹窗树结构的数据源")
    public ScienceCategoryTreeResult getDatasetTree(@RequestBody(required = false) ScienceCategoryTreeQueryDTO query) {
        return datasetService.getScienceCategoryTree(query);
    }

    @PostMapping("/api/categories/product/tree")
    @Operation(summary = "产业/产品分类树", description = "产品分类弹窗表格的数据源")
    public ProductCategoryTreeResult getProductCategoryTree(@RequestBody(required = false) ProductCategoryTreeQueryDTO query) {
        return datasetService.getProductCategoryTree(query);
    }

    @PostMapping("/api/datasets/options")
    @Operation(summary = "获取可用数据集列表", description = "上传数据页面中选择数据集下拉框数据源")
    public DatasetOptionsResult getDatasetOptions(@RequestBody(required = false) DatasetOptionsQueryDTO query) {
        return datasetService.getDatasetOptions(query);
    }

    @GetMapping("/api/datasets/my")
    @Operation(summary = "查询我创建的数据集", description = "从 token 获取当前用户，返回该用户创建的未删除数据集")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MyDatasetsResult.class)
            )
    )
    public Result<List<ManuDatasetTreeVO>> getMyDatasets() {
        return datasetService.getMyDatasets();
    }

    @GetMapping("/api/search/categories")
    @Operation(summary = "获取分类树", description = "用于获取左侧工业战略性新兴产业分类目录树形数据")
    public Result<List<CategoryTreeNode>> getSearchCategories() {
        return searchService.getCategories();
    }

    @PostMapping("/api/search/datasets")
    @Operation(summary = "数据集检索", description = "对数据集进行全文检索，并支持按分类、结果类型、分页等过滤")
    public Result<DatasetSearchResponse> searchDatasets(@RequestBody DatasetSearchRequest request) {
        return searchService.searchDatasets(request);
    }

    @GetMapping("/api/dicts/dataset-tags")
    @Operation(summary = "数据集标签列表", description = "获取数据集标签字典列表")
    public Result<List<DatasetTagVO>> getDatasetTags() {
        return datasetService.getDatasetTags();
    }

    @PostMapping("/api/data/online/schema")
    @Operation(summary = "获取在线填写表单结构", description = "上传数据页面点击在线填写并选择数据集后获取表单结构")
    public Result<OnlineFormSchemaVO> getOnlineFormSchema(@RequestBody OnlineFormSchemaQueryDTO query) {
        return datasetService.getOnlineFormSchema(query);
    }

    @PostMapping("/api/data/online/submit")
    @Operation(summary = "在线填写提交数据", description = "在线填写模式下提交多条记录")
    public Result<OnlineFormSubmitResultVO> submitOnlineFormData(@RequestBody OnlineFormSubmitDTO dto) {
        return datasetService.submitOnlineFormData(dto);
    }

    @PostMapping("/api/datasets")
    @Operation(summary = "创建数据集", description = "根据模板创建新的数据集，并返回新建数据集ID")
    public Result<CreateDatasetResultVO> createDataset(@RequestBody CreateDatasetDTO dto) {
        return datasetService.createDatasetForApi(dto);
    }

    @PostMapping("/api/data/batch/upload")
    @Operation(summary = "批量上传文件", description = "批量上传模式下上传 Excel/CSV 等文件")
    public Result<BatchUploadResultVO> importDatasetData(@RequestParam("datasetId") Integer datasetId,
                                                        @RequestPart("file") MultipartFile file) {
        return datasetService.importDatasetData(datasetId, file);
    }

    @GetMapping("/Dataset/data-page")
    @Operation(summary = "按模板分页查询数据", description = "根据数据表名称查询模板下所有数据，分页大小由前端指定")
    public Result<DatasetDataPageVO> getDatasetDataPage(@RequestParam("DatasetName") String DatasetName,
                                                       PageQuery pageQuery) {
        return datasetService.getDatasetDataPage(DatasetName, pageQuery);
    }

    @GetMapping("/api/data/batch/template")
    @Operation(summary = "批量上传-下载模板", description = "批量上传区域点击下载模板时下载固定模板文件")
    public void exportDatasetTemplate(@RequestParam("datasetId") Integer datasetId,
                                      @RequestParam("format") String format,
                                      HttpServletResponse response) {
        datasetService.exportDatasetTemplate(datasetId, format, response);
    }

    @PostMapping("/Dataset/add-column")
    @Operation(summary = "新增数据表列", description = "向指定数据表新增一列，写入Dataset_column表")
    public Result<String> addDatasetColumn(@RequestBody AddDatasetColumnDTO dto) {
        return datasetService.addDatasetColumn(dto);
    }

    @DeleteMapping("/Dataset/delete-column")
    @Operation(summary = "删除数据表列", description = "逻辑删除数据表列，并逻辑删除该列对应的数据表数据")
    public Result<String> deleteDatasetColumn(@RequestBody DeleteDatabaseColumnDTO dto) {
        return datasetService.deleteDatasetColumn(dto);
    }

    @DeleteMapping("/Dataset/delete-row")
    @Operation(summary = "删除数据表具体行", description = "按数据集名称和rowId逻辑删除该数据表下的一整行数据")
    public Result<String> deleteDatasetRow(@RequestBody DeleteDatasetRowDTO dto) {
        return datasetService.deleteDatasetRow(dto);
    }
    @PostMapping("/Dataset/audit-template")
    @Operation(summary = "审核数据集", description = "对指定数据集进行状态变更，如通过或驳回")
    public Result<AuditDatasetResultVO> auditDataset(@RequestBody AuditDatasetDTO dto) {
        return datasetService.auditDataset(dto);
    }

    @GetMapping("/Dataset/count-under-menu")
    @Operation(summary = "获取当前目录下数据集总数", description = "递归统计当前目录及其所有子目录下的数据集数量，仅统计is_menu=0且deleted=0的数据")
    public Result<Long> countDatasetsUnderMenu(@RequestParam("menuId") Integer menuId) {
        return datasetService.countDatasetsUnderMenu(menuId);
    }
}
