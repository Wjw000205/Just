package org.example.just.service.imp;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.just.context.UserContext;
import org.example.just.dao.DatasetColumnDao;
import org.example.just.dao.DatasetDataDao;
import org.example.just.dao.ManuDatasetDao;
import org.example.just.dao.ModuleColumnDao;
import org.example.just.dao.ModuleDao;
import org.example.just.dao.UserDao;
import org.example.just.dto.categoryDto.ProductCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ProductCategoryTreeResult;
import org.example.just.dto.categoryDto.ScienceCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeResult;
import org.example.just.dto.datasetDto.AddDatasetColumnDTO;
import org.example.just.dto.datasetDto.AuditDatasetDTO;
import org.example.just.dto.datasetDto.AuditDatasetResultVO;
import org.example.just.dto.datasetDto.BatchUploadResultVO;
import org.example.just.dto.datasetDto.CreateDatasetDTO;
import org.example.just.dto.datasetDto.CreateDatasetResultVO;
import org.example.just.dto.datasetDto.DatasetColumnAuditDTO;
import org.example.just.dto.datasetDto.DatasetColumnAuditVO;
import org.example.just.dto.datasetDto.DatasetTagVO;
import org.example.just.dto.datasetDto.DatasetOptionsQueryDTO;
import org.example.just.dto.datasetDto.DatasetOptionsResult;
import org.example.just.dto.datasetDto.ManuDatasetTreeVO;
import org.example.just.dto.datasetDto.OnlineFormSchemaQueryDTO;
import org.example.just.dto.datasetDto.OnlineFormSchemaVO;
import org.example.just.dto.datasetDto.OnlineFormSubmitDTO;
import org.example.just.dto.datasetDto.OnlineFormSubmitResultVO;
import org.example.just.dto.datasetDto.PendingAuditDatasetVO;
import org.example.just.dto.datasetDto.UpdateDatasetColumnDTO;
import org.example.just.entity.DatasetColumnEntity;
import org.example.just.entity.DatasetDataEntity;
import org.example.just.entity.ManuDatasetEntity;
import org.example.just.entity.ModuleColumnEntity;
import org.example.just.entity.ModuleEntity;
import org.example.just.entity.UserEntity;
import org.example.just.utils.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetServiceImpTest {

    private final ManuDatasetDao datasetDao = mock(ManuDatasetDao.class);
    private final DatasetColumnDao datasetColumnDao = mock(DatasetColumnDao.class);
    private final DatasetDataDao datasetDataDao = mock(DatasetDataDao.class);
    private final ModuleDao moduleDao = mock(ModuleDao.class);
    private final ModuleColumnDao moduleColumnDao = mock(ModuleColumnDao.class);
    private final UserDao userDao = mock(UserDao.class);
    private final DatasetServiceImp datasetService = new DatasetServiceImp(
            datasetDao,
            datasetColumnDao,
            datasetDataDao,
            moduleDao,
            moduleColumnDao,
            userDao
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createDatasetForApiCreatesDatasetFromTemplateAndReturnsDatasetId() {
        UserContext.setUserInfo(7, "alice", 1);
        CreateDatasetDTO dto = validApiDto();
        ModuleEntity module = new ModuleEntity();
        module.setId(5);
        module.setDeleted(0);
        when(moduleDao.selectOne(any())).thenReturn(module);
        when(datasetDao.selectCount(any())).thenReturn(0L);
        when(moduleColumnDao.selectList(any())).thenReturn(List.of(
                moduleColumn("材料名称", "string"),
                moduleColumn("粒径", "number")
        ));
        doAnswer(invocation -> {
            ManuDatasetEntity entity = invocation.getArgument(0);
            entity.setId(123);
            return 1;
        }).when(datasetDao).insert(any(ManuDatasetEntity.class));
        when(datasetColumnDao.insert(any(DatasetColumnEntity.class))).thenReturn(1);

        Result<CreateDatasetResultVO> result = datasetService.createDatasetForApi(dto);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData().getDatasetId()).isEqualTo(123);
        assertThat(result.getData().getName()).isEqualTo("羟基磷灰石粉末性能数据集");
        assertThat(result.getData().getCreatedAt()).isNotNull();

        ArgumentCaptor<ManuDatasetEntity> datasetCaptor = ArgumentCaptor.forClass(ManuDatasetEntity.class);
        verify(datasetDao).insert(datasetCaptor.capture());
        ManuDatasetEntity savedDataset = datasetCaptor.getValue();
        assertThat(savedDataset.getName()).isEqualTo("羟基磷灰石粉末性能数据集");
        assertThat(savedDataset.getCreator()).isEqualTo("alice");
        assertThat(savedDataset.getParent()).isEqualTo(0);
        assertThat(savedDataset.getModule()).isEqualTo(5);
        assertThat(savedDataset.getSummary()).isEqualTo("收集了不同烧结温度下羟基磷灰石粉末的粒径分布、比表面积等数据。");
        assertThat(savedDataset.getCoverUrl()).isEqualTo("https://xxx.com/cover/abc.png");
        assertThat(savedDataset.getScienceCategoryId()).isEqualTo(1001);
        assertThat(savedDataset.getProductCategoryId()).isEqualTo(3001);
        assertThat(savedDataset.getDataLevel()).isEqualTo("public");
        assertThat(savedDataset.getDataCategory()).isEqualTo("dataset");
        assertThat(savedDataset.getTemplateTagId()).isEqualTo(11);
        assertThat(savedDataset.getDatasetTagIds()).isEqualTo("[21,22,35]");

        ArgumentCaptor<DatasetColumnEntity> columnCaptor = ArgumentCaptor.forClass(DatasetColumnEntity.class);
        verify(datasetColumnDao, org.mockito.Mockito.times(2)).insert(columnCaptor.capture());
        assertThat(columnCaptor.getAllValues())
                .extracting(DatasetColumnEntity::getState)
                .containsExactly(0, 0);
        assertThat(columnCaptor.getAllValues())
                .extracting(DatasetColumnEntity::getColumnName)
                .containsExactly("材料名称", "粒径");
    }

    @Test
    void createDatasetForApiRejectsDuplicateNameWithDocumentedCode() {
        UserContext.setUserInfo(7, "alice", 1);
        when(moduleDao.selectOne(any())).thenReturn(new ModuleEntity());
        when(datasetDao.selectCount(any())).thenReturn(1L);

        Result<CreateDatasetResultVO> result = datasetService.createDatasetForApi(validApiDto());

        assertThat(result.getCode()).isEqualTo(4001);
        assertThat(result.getMessage()).isEqualTo("数据集名称已存在");
        assertThat(result.getData()).isNull();
        verify(datasetDao, never()).insert(any(ManuDatasetEntity.class));
    }

    @Test
    void getScienceCategoryTreeBuildsMenuTreeAndCountsDatasets() {
        when(datasetDao.selectList(any())).thenReturn(List.of(
                menu(1, "生物医用材料（科学）", 0),
                menu(2, "生物医用无机材料", 1),
                dataset(4, "羟基磷灰石", 2),
                menu(3, "先进基础材料", 0)
        ));
        ScienceCategoryTreeQueryDTO query = new ScienceCategoryTreeQueryDTO();
        query.setKeyword("生物");
        query.setPage(1);
        query.setPageSize(50);

        ScienceCategoryTreeResult result = datasetService.getScienceCategoryTree(query);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(1);
        assertThat(result.getData().get(0).getLevel()).isEqualTo(0);
        assertThat(result.getData().get(0).getDatasetCount()).isEqualTo(1);
        assertThat(result.getData().get(0).getChildren()).hasSize(1);
        assertThat(result.getData().get(0).getChildren().get(0).getLevel()).isEqualTo(1);
    }

    @Test
    void getProductCategoryTreeBuildsDocumentedShapeFromMenuTree() {
        when(datasetDao.selectList(any())).thenReturn(List.of(
                menu(1, "新材料产业", 0),
                menu(2, "先进钢铁材料", 1),
                menu(3, "生物医用材料", 0)
        ));
        ProductCategoryTreeQueryDTO query = new ProductCategoryTreeQueryDTO();
        query.setIndustryKeyword("新材料");

        ProductCategoryTreeResult result = datasetService.getProductCategoryTree(query);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo("p1");
        assertThat(result.getData().get(0).getIndustryCode()).isEqualTo("1");
        assertThat(result.getData().get(0).getIndustryName()).isEqualTo("新材料产业");
        assertThat(result.getData().get(0).getSectorCode()).isEqualTo("");
        assertThat(result.getData().get(0).getProductCode()).isEqualTo("");
        assertThat(result.getData().get(0).getChildren()).hasSize(1);
        assertThat(result.getData().get(0).getChildren().get(0).getId()).isEqualTo("p2");
    }

    @Test
    void getDatasetOptionsFiltersDatasetsAndCountsRows() {
        when(datasetDao.selectList(any())).thenReturn(List.of(
                datasetOption(123, "羟基磷灰石粉末性能数据集", 1001, "public"),
                datasetOption(124, "生物医用材料数据集", 1001, "private"),
                datasetOption(125, "羟基磷灰石未分类数据集", 2001, "public"),
                menu(126, "羟基磷灰石目录", 0)
        ));
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(datasetColumn(11), datasetColumn(12)));
        when(datasetDataDao.countDistinctRowIds(List.of(11, 12))).thenReturn(256L);

        DatasetOptionsQueryDTO query = new DatasetOptionsQueryDTO();
        query.setScienceCategoryIds(List.of(1001, 1002));
        query.setKeyword("羟基磷灰石");
        query.setPage(1);
        query.setPageSize(20);

        DatasetOptionsResult result = datasetService.getDatasetOptions(query);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(123);
        assertThat(result.getData().get(0).getName()).isEqualTo("羟基磷灰石粉末性能数据集");
        assertThat(result.getData().get(0).getDataLevel()).isEqualTo("public");
        assertThat(result.getData().get(0).getRecordCount()).isEqualTo(256L);
    }

    @Test
    void getMyDatasetsUsesCurrentUserFromTokenContext() {
        UserContext.setUserInfo(7, "alice", 1);
        ManuDatasetEntity ownDataset = dataset(123, "alice dataset", 0);
        ownDataset.setCreator("alice");
        ManuDatasetEntity otherDataset = dataset(124, "bob dataset", 0);
        otherDataset.setCreator("bob");
        ManuDatasetEntity ownMenu = menu(125, "alice menu", 0);
        ownMenu.setCreator("alice");
        ManuDatasetEntity deleted = dataset(126, "deleted dataset", 0);
        deleted.setCreator("alice");
        deleted.setDeleted(1);
        when(datasetDao.selectList(any())).thenReturn(List.of(ownDataset, otherDataset, ownMenu, deleted));

        Result<List<ManuDatasetTreeVO>> result = datasetService.getMyDatasets();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(123);
        assertThat(result.getData().get(0).getName()).isEqualTo("alice dataset");
        assertThat(result.getData().get(0).getCreator()).isEqualTo("alice");
        assertThat(result.getData().get(0).getIsMenu()).isEqualTo(0);
    }

    @Test
    void getMyDatasetsAlsoMatchesRealNameAndUserIdCreatorValues() {
        UserContext.setUserInfo(7, "alice", 1);
        UserEntity currentUser = new UserEntity();
        currentUser.setId(7);
        currentUser.setUsername("alice");
        currentUser.setRealName("张三");
        ManuDatasetEntity usernameCreator = dataset(123, "username dataset", 0);
        usernameCreator.setCreator("alice");
        ManuDatasetEntity realNameCreator = dataset(124, "real-name dataset", 0);
        realNameCreator.setCreator("张三");
        ManuDatasetEntity userIdCreator = dataset(125, "id dataset", 0);
        userIdCreator.setCreator("7");
        when(userDao.selectById(7)).thenReturn(currentUser);
        when(datasetDao.selectList(any())).thenReturn(List.of(usernameCreator, realNameCreator, userIdCreator));

        Result<List<ManuDatasetTreeVO>> result = datasetService.getMyDatasets();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData())
                .extracting(ManuDatasetTreeVO::getName)
                .containsExactly("username dataset", "real-name dataset", "id dataset");
    }

    @Test
    void getMyDatasetsRejectsMissingTokenUser() {
        Result<List<ManuDatasetTreeVO>> result = datasetService.getMyDatasets();

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("未登录");
        assertThat(result.getData()).isNull();
        verify(datasetDao, never()).selectList(any());
    }

    @Test
    void getPendingAuditDatasetsReturnsPendingDatasetsWithAllNonDeletedColumns() {
        ManuDatasetEntity pending = dataset(123, "dataset-a", 0);
        pending.setCreator("alice");
        pending.setAuditStatus(0);
        ManuDatasetEntity approved = dataset(124, "dataset-b", 0);
        approved.setAuditStatus(1);
        DatasetColumnEntity pendingColumn = datasetColumn(21, "pending-column", "varchar");
        pendingColumn.setDatasetName("dataset-a");
        pendingColumn.setState(0);
        DatasetColumnEntity approvedColumn = datasetColumn(22, "approved-column", "double");
        approvedColumn.setDatasetName("dataset-a");
        approvedColumn.setState(1);
        DatasetColumnEntity deletedColumn = datasetColumn(23, "deleted-column", "int");
        deletedColumn.setDatasetName("dataset-a");
        deletedColumn.setDeleted(1);
        when(datasetDao.selectList(any())).thenReturn(List.of(pending, approved));
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(pendingColumn, approvedColumn, deletedColumn));

        Result<List<PendingAuditDatasetVO>> result = datasetService.getPendingAuditDatasets();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(123);
        assertThat(result.getData().get(0).getName()).isEqualTo("dataset-a");
        assertThat(result.getData().get(0).getCreator()).isEqualTo("alice");
        assertThat(result.getData().get(0).getAuditStatus()).isEqualTo(0);
        assertThat(result.getData().get(0).getColumns())
                .extracting(DatasetColumnAuditVO::getColumnName)
                .containsExactly("pending-column", "approved-column");
        assertThat(result.getData().get(0).getColumns())
                .extracting(DatasetColumnAuditVO::getState)
                .containsExactly(0, 1);
    }

    @Test
    void getDatasetTagsBuildsDistinctTagsFromDatasetColumns() {
        DatasetColumnEntity deleted = datasetColumn(24, "已删除字段", "varchar");
        deleted.setDeleted(1);
        DatasetColumnEntity pending = datasetColumn(25, "pending", "varchar");
        pending.setState(0);
        DatasetColumnEntity rejected = datasetColumn(26, "rejected", "varchar");
        rejected.setState(-1);
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(
                datasetColumn(21, "粉末", "varchar"),
                datasetColumn(22, "烧结温度", "varchar"),
                datasetColumn(23, "粉末", "varchar"),
                deleted,
                pending,
                rejected
        ));

        Result<List<DatasetTagVO>> result = datasetService.getDatasetTags();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData())
                .extracting(DatasetTagVO::getId)
                .containsExactly(21, 22);
        assertThat(result.getData())
                .extracting(DatasetTagVO::getName)
                .containsExactly("粉末", "烧结温度");
    }

    @Test
    void getOnlineFormSchemaBuildsSectionsFromTemplateColumnBelong() {
        ManuDatasetEntity dataset = dataset(123, "羟基磷灰石粉末性能数据集", 0);
        dataset.setModule(5);
        DatasetColumnEntity pending = datasetColumn(24, "pending", "varchar");
        pending.setState(0);
        when(datasetDao.selectOne(any())).thenReturn(dataset);
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(
                datasetColumn(21, "材料编号", "varchar"),
                datasetColumn(22, "工艺参数", "text"),
                datasetColumn(23, "性能结果", "varchar")
                , pending
        ));
        when(moduleColumnDao.selectList(any())).thenReturn(List.of(
                moduleColumn(11, "材料编号", "varchar", "Object"),
                moduleColumn(12, "工艺参数", "text", "Operation"),
                moduleColumn(13, "性能结果", "varchar", "Result")
        ));

        OnlineFormSchemaQueryDTO query = new OnlineFormSchemaQueryDTO();
        query.setDatasetId(123);

        Result<OnlineFormSchemaVO> result = datasetService.getOnlineFormSchema(query);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData().getSections()).hasSize(3);
        assertThat(result.getData().getSections().get(0).getId()).isEqualTo("object");
        assertThat(result.getData().getSections().get(0).getTitle()).isEqualTo("对象区域");
        assertThat(result.getData().getSections().get(0).getFields()).hasSize(1);
        assertThat(result.getData().getSections().get(0).getFields().get(0).getId()).isEqualTo("column21");
        assertThat(result.getData().getSections().get(0).getFields().get(0).getLabel()).isEqualTo("材料编号");
        assertThat(result.getData().getSections().get(0).getFields().get(0).getType()).isEqualTo("text");
        assertThat(result.getData().getSections().get(1).getId()).isEqualTo("operation");
        assertThat(result.getData().getSections().get(1).getTitle()).isEqualTo("操作区域");
        assertThat(result.getData().getSections().get(1).getFields().get(0).getLabel()).isEqualTo("工艺参数");
        assertThat(result.getData().getSections().get(2).getId()).isEqualTo("result");
        assertThat(result.getData().getSections().get(2).getTitle()).isEqualTo("结果区域");
        assertThat(result.getData().getSections().get(2).getFields().get(0).getLabel()).isEqualTo("性能结果");
    }

    @Test
    void getPendingDatasetColumnsReturnsOnlyUnauditedColumns() {
        DatasetColumnEntity approved = datasetColumn(21, "approved", "varchar");
        DatasetColumnEntity pending = datasetColumn(22, "pending", "varchar");
        pending.setDatasetName("dataset-a");
        pending.setState(0);
        DatasetColumnEntity rejected = datasetColumn(23, "rejected", "varchar");
        rejected.setState(-1);
        DatasetColumnEntity deleted = datasetColumn(24, "deleted", "varchar");
        deleted.setState(0);
        deleted.setDeleted(1);
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(approved, pending, rejected, deleted));

        Result<List<DatasetColumnAuditVO>> result = datasetService.getPendingDatasetColumns();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(22);
        assertThat(result.getData().get(0).getColumnName()).isEqualTo("pending");
        assertThat(result.getData().get(0).getDatasetName()).isEqualTo("dataset-a");
        assertThat(result.getData().get(0).getState()).isEqualTo(0);
    }

    @Test
    void auditDatasetColumnUpdatesApprovedOrRejectedState() {
        DatasetColumnEntity column = datasetColumn(22, "pending", "varchar");
        column.setDatasetName("dataset-a");
        column.setState(0);
        when(datasetColumnDao.selectById(22)).thenReturn(column);
        when(datasetColumnDao.update(any(), any())).thenReturn(1);
        DatasetColumnAuditDTO dto = new DatasetColumnAuditDTO();
        dto.setColumnId(22);
        dto.setState(1);

        Result<DatasetColumnAuditVO> result = datasetService.auditDatasetColumn(dto);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData().getId()).isEqualTo(22);
        assertThat(result.getData().getState()).isEqualTo(1);
        verify(datasetColumnDao).update(any(), any());
    }

    @Test
    void addDatasetColumnCreatesPendingColumn() {
        when(datasetDao.selectOne(any())).thenReturn(dataset(123, "dataset-a", 0));
        when(datasetColumnDao.selectOne(any())).thenReturn(null);
        when(datasetColumnDao.insert(any(DatasetColumnEntity.class))).thenReturn(1);
        AddDatasetColumnDTO dto = new AddDatasetColumnDTO();
        dto.setDatasetName("dataset-a");
        dto.setColumnName("new-column");
        dto.setColumnType("varchar");

        Result<String> result = datasetService.addDatasetColumn(dto);

        assertThat(result.getCode()).isEqualTo(200);
        ArgumentCaptor<DatasetColumnEntity> columnCaptor = ArgumentCaptor.forClass(DatasetColumnEntity.class);
        verify(datasetColumnDao).insert(columnCaptor.capture());
        assertThat(columnCaptor.getValue().getDatasetName()).isEqualTo("dataset-a");
        assertThat(columnCaptor.getValue().getColumnName()).isEqualTo("new-column");
        assertThat(columnCaptor.getValue().getColumnType()).isEqualTo("varchar");
        assertThat(columnCaptor.getValue().getState()).isEqualTo(0);
    }

    @Test
    void updateDatasetColumnChangesNameAndTypeThenMarksPending() {
        DatasetColumnEntity column = datasetColumn(22, "old-column", "varchar");
        column.setDatasetName("dataset-a");
        column.setState(1);
        when(datasetColumnDao.selectById(22)).thenReturn(column);
        when(datasetColumnDao.selectOne(any())).thenReturn(null);
        when(datasetColumnDao.update(any(), any())).thenReturn(1);
        UpdateDatasetColumnDTO dto = new UpdateDatasetColumnDTO();
        dto.setColumnId(22);
        dto.setColumnName("new-column");
        dto.setColumnType("double");

        Result<DatasetColumnAuditVO> result = datasetService.updateDatasetColumn(dto);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData().getId()).isEqualTo(22);
        assertThat(result.getData().getDatasetName()).isEqualTo("dataset-a");
        assertThat(result.getData().getColumnName()).isEqualTo("new-column");
        assertThat(result.getData().getColumnType()).isEqualTo("double");
        assertThat(result.getData().getState()).isEqualTo(0);
        verify(datasetColumnDao).update(any(), any());
    }

    @Test
    void auditDatasetApprovesAllColumnsWhenDatasetApproved() {
        ManuDatasetEntity dataset = dataset(123, "dataset-a", 0);
        when(datasetDao.selectOne(any())).thenReturn(dataset);
        when(datasetDao.update(any(), any())).thenReturn(1);
        when(datasetColumnDao.update(any(), any())).thenReturn(2);
        AuditDatasetDTO dto = new AuditDatasetDTO();
        dto.setDatasetName("dataset-a");
        dto.setStatus(1);
        dto.setAuditor("admin");
        dto.setRemark("审核通过");

        Result<AuditDatasetResultVO> result = datasetService.auditDataset(dto);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getStatus()).isEqualTo(1);
        verify(datasetColumnDao).update(any(), any());
    }

    @Test
    void submitOnlineFormDataPersistsValidRecordsAndReportsInvalidRows() {
        ManuDatasetEntity dataset = dataset(123, "online_dataset", 0);
        when(datasetDao.selectOne(any())).thenReturn(dataset);
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(
                datasetColumn(21, "sampleId", "varchar"),
                datasetColumn(22, "sinterTemp", "double")
        ));
        when(datasetDataDao.selectMaxRowIdByColumnIds(any())).thenReturn(7);
        when(datasetDataDao.insertBatch(any())).thenReturn(2);

        OnlineFormSubmitDTO dto = new OnlineFormSubmitDTO();
        dto.setDatasetId(123);
        dto.setRecords(List.of(
                Map.of("column21", "S-001", "column22", 1200),
                Map.of("column21", "S-002", "column22", "")
        ));

        Result<OnlineFormSubmitResultVO> result = datasetService.submitOnlineFormData(dto);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("partially_success");
        assertThat(result.getData().getAcceptedCount()).isEqualTo(1);
        assertThat(result.getData().getFailedCount()).isEqualTo(1);
        assertThat(result.getData().getErrors()).hasSize(1);
        assertThat(result.getData().getErrors().get(0).getRowIndex()).isEqualTo(1);
        assertThat(result.getData().getErrors().get(0).getField()).isEqualTo("column22");
        assertThat(result.getData().getErrors().get(0).getMessage()).isEqualTo("sinterTemp不能为空");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DatasetDataEntity>> dataCaptor = ArgumentCaptor.forClass((Class<List<DatasetDataEntity>>) (Class<?>) List.class);
        verify(datasetDataDao).insertBatch(dataCaptor.capture());
        assertThat(dataCaptor.getValue()).hasSize(2);
        assertThat(dataCaptor.getValue())
                .extracting(DatasetDataEntity::getColumnId)
                .containsExactly(21, 22);
        assertThat(dataCaptor.getValue())
                .extracting(DatasetDataEntity::getRowId)
                .containsExactly(8, 8);
        assertThat(dataCaptor.getValue())
                .extracting(DatasetDataEntity::getData)
                .containsExactly("S-001", "1200");
    }

    @Test
    void importDatasetDataParsesTwoRowGroupedExcelHeaders() throws Exception {
        ManuDatasetEntity dataset = dataset(123, "batch_dataset", 0);
        dataset.setModule(5);
        when(datasetDao.selectOne(any())).thenReturn(dataset);
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(
                datasetColumn(21, "obj1", "varchar"),
                datasetColumn(22, "obj2", "varchar"),
                datasetColumn(23, "operation1", "varchar"),
                datasetColumn(24, "result1", "varchar")
        ));
        when(moduleColumnDao.selectList(any())).thenReturn(List.of(
                moduleColumn(11, "obj1", "varchar", "Object"),
                moduleColumn(12, "obj2", "varchar", "Object"),
                moduleColumn(13, "operation1", "varchar", "Operation"),
                moduleColumn(14, "result1", "varchar", "Result")
        ));
        when(datasetDataDao.selectMaxRowIdByColumnIds(any())).thenReturn(7);
        when(datasetDataDao.insertBatch(any())).thenReturn(4);

        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("template");
            Row groupRow = sheet.createRow(0);
            groupRow.createCell(0).setCellValue("object");
            groupRow.createCell(2).setCellValue("operation");
            groupRow.createCell(3).setCellValue("result");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

            Row fieldRow = sheet.createRow(1);
            fieldRow.createCell(0).setCellValue("obj1");
            fieldRow.createCell(1).setCellValue("obj2");
            fieldRow.createCell(2).setCellValue("operation1");
            fieldRow.createCell(3).setCellValue("result1");

            Row firstDataRow = sheet.createRow(2);
            firstDataRow.createCell(0).setCellValue("S-001");
            firstDataRow.createCell(1).setCellValue("material-a");
            firstDataRow.createCell(2).setCellValue("1200");
            firstDataRow.createCell(3).setCellValue("15.3");

            Row duplicatedDataRow = sheet.createRow(3);
            duplicatedDataRow.createCell(0).setCellValue("S-001");
            duplicatedDataRow.createCell(1).setCellValue("material-b");
            duplicatedDataRow.createCell(2).setCellValue("1250");
            duplicatedDataRow.createCell(3).setCellValue("14.8");

            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "batch.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );

        Result<BatchUploadResultVO> result = datasetService.importDatasetData(123, file);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData().getTaskId()).startsWith("job-");
        assertThat(result.getData().getAcceptedCount()).isEqualTo(1);
        assertThat(result.getData().getFailedCount()).isEqualTo(1);
        assertThat(result.getData().getErrors()).hasSize(1);
        assertThat(result.getData().getErrors().get(0).getRowIndex()).isEqualTo(4);
        assertThat(result.getData().getErrors().get(0).getMessage()).isEqualTo("obj1重复");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DatasetDataEntity>> dataCaptor = ArgumentCaptor.forClass((Class<List<DatasetDataEntity>>) (Class<?>) List.class);
        verify(datasetDataDao).insertBatch(dataCaptor.capture());
        assertThat(dataCaptor.getValue()).hasSize(4);
        assertThat(dataCaptor.getValue())
                .extracting(DatasetDataEntity::getColumnId)
                .containsExactly(21, 22, 23, 24);
        assertThat(dataCaptor.getValue())
                .extracting(DatasetDataEntity::getRowId)
                .containsExactly(8, 8, 8, 8);
        assertThat(dataCaptor.getValue())
                .extracting(DatasetDataEntity::getData)
                .containsExactly("S-001", "material-a", "1200", "15.3");
    }

    @Test
    void exportDatasetTemplateExcelWritesGroupedTwoRowHeaders() throws Exception {
        ManuDatasetEntity dataset = dataset(123, "batch_dataset", 0);
        dataset.setModule(5);
        when(datasetDao.selectOne(any())).thenReturn(dataset);
        when(datasetColumnDao.selectList(any())).thenReturn(List.of(
                datasetColumn(21, "obj1", "varchar"),
                datasetColumn(22, "obj2", "varchar"),
                datasetColumn(23, "obj3", "varchar"),
                datasetColumn(24, "operation1", "varchar"),
                datasetColumn(25, "operation2", "varchar"),
                datasetColumn(26, "result1", "varchar"),
                datasetColumn(27, "result2", "varchar"),
                datasetColumn(28, "result3", "varchar")
        ));
        when(moduleColumnDao.selectList(any())).thenReturn(List.of(
                moduleColumn(11, "obj1", "varchar", "Object"),
                moduleColumn(12, "obj2", "varchar", "Object"),
                moduleColumn(13, "obj3", "varchar", "Object"),
                moduleColumn(14, "operation1", "varchar", "Operation"),
                moduleColumn(15, "operation2", "varchar", "Operation"),
                moduleColumn(16, "result1", "varchar", "Result"),
                moduleColumn(17, "result2", "varchar", "Result"),
                moduleColumn(18, "result3", "varchar", "Result")
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();

        datasetService.exportDatasetTemplate(123, "excel", response);

        assertThat(response.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition")).contains("batch_dataset_template.xlsx");
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("object");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("operation");
            assertThat(sheet.getRow(0).getCell(5).getStringCellValue()).isEqualTo("result");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("obj1");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("obj2");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("obj3");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("operation1");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("operation2");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("result1");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo("result2");
            assertThat(sheet.getRow(1).getCell(7).getStringCellValue()).isEqualTo("result3");
            assertThat(sheet.getMergedRegions()).hasSize(3);
        }
    }

    private CreateDatasetDTO validApiDto() {
        CreateDatasetDTO dto = new CreateDatasetDTO();
        dto.setName(" 羟基磷灰石粉末性能数据集 ");
        dto.setSummary("收集了不同烧结温度下羟基磷灰石粉末的粒径分布、比表面积等数据。");
        dto.setCoverUrl("https://xxx.com/cover/abc.png");
        dto.setScienceCategoryId(1001);
        dto.setProductCategoryId(3001);
        dto.setDataLevel("public");
        dto.setDataCategory("dataset");
        dto.setTemplateTagId(11);
        dto.setTemplateId(5);
        dto.setDatasetTagIds(List.of(21, 22, 35));
        return dto;
    }

    private ModuleColumnEntity moduleColumn(String name, String type) {
        ModuleColumnEntity entity = new ModuleColumnEntity();
        entity.setColumnName(name);
        entity.setType(type);
        entity.setDeleted(0);
        return entity;
    }

    private ModuleColumnEntity moduleColumn(Integer id, String name, String type, String belong) {
        ModuleColumnEntity entity = moduleColumn(name, type);
        entity.setId(id);
        entity.setBelong(belong);
        return entity;
    }

    private DatasetColumnEntity datasetColumn(Integer id) {
        DatasetColumnEntity entity = new DatasetColumnEntity();
        entity.setId(id);
        entity.setDeleted(0);
        entity.setState(1);
        return entity;
    }

    private DatasetColumnEntity datasetColumn(Integer id, String name, String type) {
        DatasetColumnEntity entity = datasetColumn(id);
        entity.setColumnName(name);
        entity.setColumnType(type);
        return entity;
    }

    private ManuDatasetEntity menu(Integer id, String name, Integer parent) {
        ManuDatasetEntity entity = new ManuDatasetEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setParent(parent);
        entity.setIsMenu(1);
        entity.setDeleted(0);
        return entity;
    }

    private ManuDatasetEntity datasetOption(Integer id, String name, Integer scienceCategoryId, String dataLevel) {
        ManuDatasetEntity entity = dataset(id, name, 0);
        entity.setScienceCategoryId(scienceCategoryId);
        entity.setDataLevel(dataLevel);
        return entity;
    }

    private ManuDatasetEntity dataset(Integer id, String name, Integer parent) {
        ManuDatasetEntity entity = new ManuDatasetEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setParent(parent);
        entity.setIsMenu(0);
        entity.setDeleted(0);
        return entity;
    }
}
