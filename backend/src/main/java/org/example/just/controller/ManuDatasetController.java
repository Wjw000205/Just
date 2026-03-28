package org.example.just.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.datasetDto.*;
import org.example.just.service.DatasetService;
import org.example.just.utils.PageQuery;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/Dataset")
@Tag(name = "目录数据库操作接口", description = "模板目录相关操作")
public class ManuDatasetController {

    private final DatasetService DatasetService;

    public ManuDatasetController(DatasetService DatasetService) {
        this.DatasetService = DatasetService;
    }

    @PostMapping("/create-menu")
    @Operation(summary = "创建目录", description = "仅向 Dataset 表插入一条目录记录")
    public Result<String> createMenu(@RequestBody CreateMenuDatasetDTO dto) {
        return DatasetService.createMenu(dto);
    }

    @GetMapping("/getManuList")
    @Operation(summary = "查询模板目录树", description = "按父子关系返回模板目录树形结构")
    public Result<List<ManuDatasetTreeVO>> getDatasetTree() {
        return DatasetService.getDatasetTree();
    }

    @PostMapping("/create-template")
    @Operation(summary = "创建数据集", description = "模板名称、父目录、所属module写入Dataset表，模板列写入Dataset_column表")
    public Result<String> createDataset(@RequestBody CreateDatasetDTO dto) {
        return DatasetService.createDataset(dto);
    }

    @PostMapping("/import-data")
    @Operation(summary = "导入数据表数据", description = "上传Excel，将数据按数据集列映射后批量插入Dataset_data表")
    public Result<String> importDatasetData(@RequestParam("DatasetName") String DatasetName,
                                           @RequestPart("file") MultipartFile file) {
        return DatasetService.importDatasetData(DatasetName, file);
    }

    @GetMapping("/data-page")
    @Operation(summary = "按模板分页查询数据", description = "根据数据表名称查询模板下所有数据，分页大小由前端指定")
    public Result<DatasetDataPageVO> getDatasetDataPage(@RequestParam("DatasetName") String DatasetName,
                                                       PageQuery pageQuery) {
        return DatasetService.getDatasetDataPage(DatasetName, pageQuery);
    }

    @GetMapping("/export-template")
    @Operation(summary = "导出数据表结构", description = "只导出数据集列为Excel表头，不导出具体数据")
    public void exportDatasetTemplate(@RequestParam("DatasetName") String DatasetName,
                                     HttpServletResponse response) {
        DatasetService.exportDatasetTemplate(DatasetName, response);
    }

    @PostMapping("/add-column")
    @Operation(summary = "新增数据表列", description = "向指定数据表新增一列，写入Dataset_column表")
    public Result<String> addDatasetColumn(@RequestBody AddDatasetColumnDTO dto) {
        return DatasetService.addDatasetColumn(dto);
    }

    @DeleteMapping("/delete-column")
    @Operation(summary = "删除数据表列", description = "逻辑删除数据表列，并逻辑删除该列对应的数据表数据")
    public Result<String> deleteDatasetColumn(@RequestBody DeleteDatabaseColumnDTO dto) {
        return DatasetService.deleteDatasetColumn(dto);
    }

    @DeleteMapping("/delete-row")
    @Operation(summary = "删除数据表具体行", description = "按数据集名称和rowId逻辑删除该数据表下的一整行数据")
    public Result<String> deleteDatasetRow(@RequestBody DeleteDatasetRowDTO dto) {
        return DatasetService.deleteDatasetRow(dto);
    }
    @PostMapping("/audit-template")
    @Operation(summary = "审核数据集", description = "对指定数据集进行状态变更，如通过或驳回")
    public Result<AuditDatasetResultVO> auditDataset(@RequestBody AuditDatasetDTO dto) {
        return DatasetService.auditDataset(dto);
    }
}