package org.example.just.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.datasetDto.*;
import org.example.just.utils.PageQuery;
import org.example.just.utils.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatasetService {

    Result<String> createMenu(CreateMenuDatasetDTO dto);

    Result<List<ManuDatasetTreeVO>> getDatasetTree();

    Result<String> createDataset(CreateDatasetDTO dto);

    Result<String> importDatasetData(String DatasetName, MultipartFile file);

    Result<DatasetDataPageVO> getDatasetDataPage(String DatasetName, PageQuery pageQuery);

    void exportDatasetTemplate(String DatasetName, HttpServletResponse response);

    Result<String> addDatasetColumn(AddDatasetColumnDTO dto);

    Result<String> deleteDatasetColumn(DeleteDatabaseColumnDTO dto);

    Result<String> deleteDatasetRow(DeleteDatasetRowDTO dto);

    Result<AuditDatasetResultVO> auditDataset(AuditDatasetDTO dto);

    Result<Long> countDatasetsUnderMenu(Integer menuId);

    Long getTotal();
}