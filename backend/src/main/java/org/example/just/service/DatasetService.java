package org.example.just.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.just.dto.categoryDto.ProductCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ProductCategoryTreeResult;
import org.example.just.dto.categoryDto.ScienceCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeResult;
import org.example.just.dto.datasetDto.*;
import org.example.just.utils.PageQuery;
import org.example.just.utils.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatasetService {

    Result<String> createMenu(CreateMenuDatasetDTO dto);

    Result<List<ManuDatasetTreeVO>> getDatasetTree();

    ScienceCategoryTreeResult getScienceCategoryTree(ScienceCategoryTreeQueryDTO query);

    ProductCategoryTreeResult getProductCategoryTree(ProductCategoryTreeQueryDTO query);

    DatasetOptionsResult getDatasetOptions(DatasetOptionsQueryDTO query);

    Result<List<DatasetTagVO>> getDatasetTags();

    Result<OnlineFormSchemaVO> getOnlineFormSchema(OnlineFormSchemaQueryDTO query);

    Result<OnlineFormSubmitResultVO> submitOnlineFormData(OnlineFormSubmitDTO dto);

    Result<CreateDatasetResultVO> createDatasetForApi(CreateDatasetDTO dto);

    Result<BatchUploadResultVO> importDatasetData(Integer datasetId, MultipartFile file);

    Result<DatasetDataPageVO> getDatasetDataPage(String DatasetName, PageQuery pageQuery);

    void exportDatasetTemplate(Integer datasetId, String format, HttpServletResponse response);

    Result<String> addDatasetColumn(AddDatasetColumnDTO dto);

    Result<String> deleteDatasetColumn(DeleteDatabaseColumnDTO dto);

    Result<String> deleteDatasetRow(DeleteDatasetRowDTO dto);

    Result<AuditDatasetResultVO> auditDataset(AuditDatasetDTO dto);

    Result<Long> countDatasetsUnderMenu(Integer menuId);

    Long getTotal();
}
