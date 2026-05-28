package org.example.just.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.just.dto.datasetDto.DatasetOptionsResult;
import org.example.just.dto.datasetDto.DatasetOptionsVO;
import org.example.just.dto.datasetDto.CreateDatasetResultVO;
import org.example.just.dto.datasetDto.OnlineFormFieldOptionVO;
import org.example.just.dto.datasetDto.OnlineFormFieldVO;
import org.example.just.dto.datasetDto.OnlineFormSchemaVO;
import org.example.just.dto.datasetDto.OnlineFormSectionVO;
import org.example.just.service.DatasetService;
import org.example.just.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManuDatasetControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getOnlineFormSchemaUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        OnlineFormFieldVO materialCode = new OnlineFormFieldVO();
        materialCode.setId("materialCode");
        materialCode.setLabel("材料编号");
        materialCode.setType("text");
        materialCode.setRequired(true);
        materialCode.setPlaceholder("");
        materialCode.setDescription("");

        OnlineFormFieldVO dataSource = new OnlineFormFieldVO();
        dataSource.setId("dataSource");
        dataSource.setLabel("数据来源");
        dataSource.setType("select");
        dataSource.setRequired(true);
        dataSource.setPlaceholder("请选择");
        dataSource.setDescription("选取一种类型以生成对应的表单");
        dataSource.setOptions(List.of(
                new OnlineFormFieldOptionVO("experiment", "实验测量"),
                new OnlineFormFieldOptionVO("simulation", "数值模拟")
        ));

        OnlineFormSectionVO section = new OnlineFormSectionVO();
        section.setId("object");
        section.setTitle("对象区域");
        section.setSubtitle("");
        section.setFields(List.of(materialCode, dataSource));

        OnlineFormSchemaVO schema = new OnlineFormSchemaVO();
        schema.setSections(List.of(section));
        when(datasetService.getOnlineFormSchema(any()))
                .thenReturn(Result.success(0, "success", schema));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService))
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
                .andExpect(jsonPath("$.data.sections[0].fields[0].id").value("materialCode"))
                .andExpect(jsonPath("$.data.sections[0].fields[0].label").value("材料编号"))
                .andExpect(jsonPath("$.data.sections[0].fields[1].id").value("dataSource"))
                .andExpect(jsonPath("$.data.sections[0].fields[1].options[0].value").value("experiment"));

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
                .standaloneSetup(new ManuDatasetController(datasetService))
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
    void createDatasetApiUsesDocumentedPathAndReturnsDatasetId() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        CreateDatasetResultVO resultVO = new CreateDatasetResultVO();
        resultVO.setDatasetId(123);
        resultVO.setName("羟基磷灰石粉末性能数据集");
        Result<CreateDatasetResultVO> result = Result.success(0, "success", resultVO);
        when(datasetService.createDatasetForApi(any())).thenReturn(result);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService))
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
    void legacyCreateDatasetPathIsRemoved() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService))
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
}
