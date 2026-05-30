package org.example.just.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.just.dto.datasetDto.BatchUploadResultVO;
import org.example.just.dto.datasetDto.CategoryTreeNode;
import org.example.just.dto.datasetDto.DatasetTagVO;
import org.example.just.dto.datasetDto.DatasetOptionsResult;
import org.example.just.dto.datasetDto.DatasetOptionsVO;
import org.example.just.dto.datasetDto.DatasetColumnAuditDTO;
import org.example.just.dto.datasetDto.DatasetColumnAuditVO;
import org.example.just.dto.datasetDto.CreateDatasetResultVO;
import org.example.just.dto.datasetDto.DatasetSearchRequest;
import org.example.just.dto.datasetDto.DatasetSearchResponse;
import org.example.just.dto.datasetDto.ManuDatasetTreeVO;
import org.example.just.dto.datasetDto.MyDatasetsResult;
import org.example.just.dto.datasetDto.OnlineFormFieldVO;
import org.example.just.dto.datasetDto.OnlineFormSchemaVO;
import org.example.just.dto.datasetDto.OnlineFormSectionVO;
import org.example.just.dto.datasetDto.OnlineFormSubmitResultVO;
import org.example.just.service.DatasetService;
import org.example.just.service.SearchService;
import org.example.just.utils.Result;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class ManuDatasetControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getOnlineFormSchemaUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        OnlineFormFieldVO materialCode = new OnlineFormFieldVO();
        materialCode.setId("column11");
        materialCode.setLabel("材料编号");
        materialCode.setType("text");
        materialCode.setRequired(true);
        materialCode.setPlaceholder("");
        materialCode.setDescription("");

        OnlineFormFieldVO processParam = new OnlineFormFieldVO();
        processParam.setId("column12");
        processParam.setLabel("工艺参数");
        processParam.setType("text");
        processParam.setRequired(true);
        processParam.setPlaceholder("");
        processParam.setDescription("");

        OnlineFormSectionVO objectSection = new OnlineFormSectionVO();
        objectSection.setId("object");
        objectSection.setTitle("对象区域");
        objectSection.setSubtitle("");
        objectSection.setFields(List.of(materialCode));

        OnlineFormSectionVO operationSection = new OnlineFormSectionVO();
        operationSection.setId("operation");
        operationSection.setTitle("操作区域");
        operationSection.setSubtitle("");
        operationSection.setFields(List.of(processParam));

        OnlineFormSchemaVO schema = new OnlineFormSchemaVO();
        schema.setSections(List.of(objectSection, operationSection));
        when(datasetService.getOnlineFormSchema(any()))
                .thenReturn(Result.success(0, "success", schema));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        ApiOnlineFormSchemaBody body = new ApiOnlineFormSchemaBody();
        body.datasetId = 123;

        mockMvc.perform(post("/api/data/online/schema")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.sections[0].id").value("object"))
                .andExpect(jsonPath("$.data.sections[0].title").value("对象区域"))
                .andExpect(jsonPath("$.data.sections[0].fields[0].id").value("column11"))
                .andExpect(jsonPath("$.data.sections[0].fields[0].label").value("材料编号"))
                .andExpect(jsonPath("$.data.sections[1].id").value("operation"))
                .andExpect(jsonPath("$.data.sections[1].fields[0].id").value("column12"));

        verify(datasetService).getOnlineFormSchema(any());
    }

    @Test
    void getDatasetOptionsUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        DatasetOptionsVO option = new DatasetOptionsVO();
        option.setId(123);
        option.setName("羟基磷灰石粉末性能数据集");
        option.setDataLevel("public");
        option.setRecordCount(256L);
        when(datasetService.getDatasetOptions(any()))
                .thenReturn(DatasetOptionsResult.success(List.of(option), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        ApiDatasetOptionsBody body = new ApiDatasetOptionsBody();
        body.scienceCategoryIds = new Integer[]{1001, 1002};
        body.keyword = "羟基磷灰石";
        body.page = 1;
        body.pageSize = 20;

        mockMvc.perform(post("/api/datasets/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(123))
                .andExpect(jsonPath("$.data[0].name").value("羟基磷灰石粉末性能数据集"))
                .andExpect(jsonPath("$.data[0].dataLevel").value("public"))
                .andExpect(jsonPath("$.data[0].recordCount").value(256))
                .andExpect(jsonPath("$.total").value(1));

        verify(datasetService).getDatasetOptions(any());
    }

    @Test
    void getDatasetTagsUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        when(datasetService.getDatasetTags())
                .thenReturn(Result.success(0, "success", List.of(
                        new DatasetTagVO(21, "粉末"),
                        new DatasetTagVO(22, "烧结温度")
                )));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(get("/api/dicts/dataset-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(21))
                .andExpect(jsonPath("$.data[0].name").value("粉末"))
                .andExpect(jsonPath("$.data[1].id").value(22))
                .andExpect(jsonPath("$.data[1].name").value("烧结温度"));

        verify(datasetService).getDatasetTags();
    }

    @Test
    void getMyDatasetsUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        ManuDatasetTreeVO dataset = new ManuDatasetTreeVO();
        dataset.setId(123);
        dataset.setName("alice dataset");
        dataset.setCreator("alice");
        dataset.setParent(0);
        dataset.setIsMenu(0);
        when(datasetService.getMyDatasets())
                .thenReturn(Result.success(0, "success", List.of(dataset)));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(get("/api/datasets/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(123))
                .andExpect(jsonPath("$.data[0].name").value("alice dataset"))
                .andExpect(jsonPath("$.data[0].creator").value("alice"))
                .andExpect(jsonPath("$.data[0].isMenu").value(0));

        verify(datasetService).getMyDatasets();
    }

    @Test
    void getMyDatasetsDocumentsDataAsDatasetList() throws Exception {
        Method method = ManuDatasetController.class.getMethod("getMyDatasets");
        ApiResponse apiResponse = method.getAnnotation(ApiResponse.class);
        Field dataField = MyDatasetsResult.class.getDeclaredField("data");

        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.content()[0].schema().implementation()).isEqualTo(MyDatasetsResult.class);
        assertThat(dataField.getGenericType().getTypeName())
                .contains("java.util.List")
                .contains("ManuDatasetTreeVO");
    }

    @Test
    void getPendingDatasetColumnsUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        DatasetColumnAuditVO column = new DatasetColumnAuditVO();
        column.setId(22);
        column.setDatasetName("dataset-a");
        column.setColumnName("pending");
        column.setColumnType("varchar");
        column.setState(0);
        when(datasetService.getPendingDatasetColumns())
                .thenReturn(Result.success(0, "success", List.of(column)));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(get("/api/data/columns/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(22))
                .andExpect(jsonPath("$.data[0].datasetName").value("dataset-a"))
                .andExpect(jsonPath("$.data[0].columnName").value("pending"))
                .andExpect(jsonPath("$.data[0].state").value(0));

        verify(datasetService).getPendingDatasetColumns();
    }

    @Test
    void auditDatasetColumnUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        DatasetColumnAuditVO column = new DatasetColumnAuditVO();
        column.setId(22);
        column.setDatasetName("dataset-a");
        column.setColumnName("pending");
        column.setColumnType("varchar");
        column.setState(1);
        when(datasetService.auditDatasetColumn(any()))
                .thenReturn(Result.success(0, "success", column));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        DatasetColumnAuditDTO body = new DatasetColumnAuditDTO();
        body.setColumnId(22);
        body.setState(1);

        mockMvc.perform(post("/api/data/columns/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(22))
                .andExpect(jsonPath("$.data.datasetName").value("dataset-a"))
                .andExpect(jsonPath("$.data.state").value(1));

        verify(datasetService).auditDatasetColumn(any());
    }

    @Test
    void createDatasetApiUsesDocumentedPathAndReturnsDatasetId() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        CreateDatasetResultVO resultVO = new CreateDatasetResultVO();
        resultVO.setDatasetId(123);
        resultVO.setName("羟基磷灰石粉末性能数据集");
        Result<CreateDatasetResultVO> result = Result.success(0, "success", resultVO);
        when(datasetService.createDatasetForApi(any())).thenReturn(result);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(post("/api/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validApiBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.datasetId").value(123));

        verify(datasetService).createDatasetForApi(any());
    }

    @Test
    void submitOnlineFormDataUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        OnlineFormSubmitResultVO resultVO = new OnlineFormSubmitResultVO();
        resultVO.setAcceptedCount(2);
        resultVO.setFailedCount(0);
        resultVO.setErrors(List.of());
        when(datasetService.submitOnlineFormData(any()))
                .thenReturn(Result.success(0, "success", resultVO));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        ApiOnlineFormSubmitBody body = new ApiOnlineFormSubmitBody();
        body.datasetId = 123;
        body.records = List.of(
                Map.of("column21", "S-001", "column22", 1200),
                Map.of("column21", "S-002", "column22", 1250)
        );

        mockMvc.perform(post("/api/data/online/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.acceptedCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.errors").isArray());

        verify(datasetService).submitOnlineFormData(any());
    }

    @Test
    void exportDatasetTemplateUsesDocumentedPathAndQueryParams() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(get("/api/data/batch/template")
                        .param("datasetId", "123")
                        .param("format", "excel"))
                .andExpect(status().isOk());

        verify(datasetService).exportDatasetTemplate(eq(123), eq("excel"), any(HttpServletResponse.class));
    }

    @Test
    void importDatasetDataUsesDocumentedBatchUploadPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        BatchUploadResultVO resultVO = new BatchUploadResultVO();
        resultVO.setTaskId("job-202603160001");
        resultVO.setAcceptedCount(100);
        resultVO.setFailedCount(3);
        resultVO.setErrors(List.of());
        when(datasetService.importDatasetData(any(), any()))
                .thenReturn(Result.success(0, "success", resultVO));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/data/batch/upload")
                        .file(file)
                        .param("datasetId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskId").value("job-202603160001"))
                .andExpect(jsonPath("$.data.acceptedCount").value(100))
                .andExpect(jsonPath("$.data.failedCount").value(3))
                .andExpect(jsonPath("$.data.errors").isArray());

        verify(datasetService).importDatasetData(eq(123), any());
    }

    @Test
    void searchCategoriesUsesManuDatasetControllerAndSearchService() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        SearchService searchService = mock(SearchService.class);
        CategoryTreeNode node = new CategoryTreeNode();
        node.setId("c1");
        node.setName("新材料产业");
        node.setChildren(List.of());
        when(searchService.getCategories())
                .thenReturn(Result.success("success", List.of(node)));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService, searchService))
                .build();

        mockMvc.perform(get("/api/search/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value("c1"))
                .andExpect(jsonPath("$.data[0].name").value("新材料产业"));

        verify(searchService).getCategories();
    }

    @Test
    void searchDatasetsUsesManuDatasetControllerAndSearchService() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        SearchService searchService = mock(SearchService.class);
        DatasetSearchResponse response = new DatasetSearchResponse();
        response.setPage(1);
        response.setPageSize(10);
        response.setTotal(1);
        DatasetSearchResponse.DatasetItem item = new DatasetSearchResponse.DatasetItem();
        item.setId("123");
        item.setTitle("羟基磷灰石数据集");
        response.setItems(List.of(item));
        when(searchService.searchDatasets(any()))
                .thenReturn(Result.success("success", response));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService, searchService))
                .build();

        DatasetSearchRequest request = new DatasetSearchRequest();
        request.setKeyword("羟基磷灰石");
        request.setResultType("dataset");

        mockMvc.perform(post("/api/search/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("123"))
                .andExpect(jsonPath("$.data.items[0].title").value("羟基磷灰石数据集"));

        verify(searchService).searchDatasets(any());
    }

    @Test
    void legacyImportDataPathIsRemoved() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "legacy.xlsx", "application/octet-stream", new byte[]{1});
        mockMvc.perform(multipart("/Dataset/import-data")
                        .file(file)
                        .param("DatasetName", "legacy"))
                .andExpect(status().isNotFound());

        verify(datasetService, never()).importDatasetData(any(), any());
    }

    @Test
    void legacyExportTemplatePathIsRemoved() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(get("/Dataset/export-template")
                        .param("DatasetName", "legacy"))
                .andExpect(status().isNotFound());

        verify(datasetService, never()).exportDatasetTemplate(any(), any(), any(HttpServletResponse.class));
    }

    @Test
    void legacyCreateDatasetPathIsRemoved() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(datasetService))
                .build();

        mockMvc.perform(post("/Dataset/create-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"legacy\",\"creator\":\"admin\",\"parent\":1,\"module\":1,\"columns\":[]}"))
                .andExpect(status().isNotFound());

        verify(datasetService, never()).createDatasetForApi(any());
    }

    private ApiCreateDatasetBody validApiBody() {
        ApiCreateDatasetBody body = new ApiCreateDatasetBody();
        body.name = "羟基磷灰石粉末性能数据集";
        body.summary = "收集了不同烧结温度下羟基磷灰石粉末的粒径分布、比表面积等数据。";
        body.coverUrl = "https://xxx.com/cover/abc.png";
        body.scienceCategoryId = 1001;
        body.productCategoryId = 3001;
        body.dataLevel = "public";
        body.dataCategory = "dataset";
        body.templateTagId = 11;
        body.templateId = 5;
        body.datasetTagIds = new Integer[]{21, 22, 35};
        return body;
    }

    private ManuDatasetController controller(DatasetService datasetService) {
        return new ManuDatasetController(datasetService, mock(SearchService.class));
    }

    private static class ApiCreateDatasetBody {
        public String name;
        public String summary;
        public String coverUrl;
        public Integer scienceCategoryId;
        public Integer productCategoryId;
        public String dataLevel;
        public String dataCategory;
        public Integer templateTagId;
        public Integer templateId;
        public Integer[] datasetTagIds;
    }

    private static class ApiDatasetOptionsBody {
        public Integer[] scienceCategoryIds;
        public String keyword;
        public Integer page;
        public Integer pageSize;
    }

    private static class ApiOnlineFormSchemaBody {
        public Integer datasetId;
    }

    private static class ApiOnlineFormSubmitBody {
        public Integer datasetId;
        public List<Map<String, Object>> records;
    }
}
