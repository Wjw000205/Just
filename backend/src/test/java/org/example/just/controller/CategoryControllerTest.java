package org.example.just.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.just.dto.categoryDto.ProductCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ProductCategoryTreeResult;
import org.example.just.dto.categoryDto.ProductCategoryTreeVO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeQueryDTO;
import org.example.just.dto.categoryDto.ScienceCategoryTreeResult;
import org.example.just.dto.categoryDto.ScienceCategoryTreeVO;
import org.example.just.service.DatasetService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getScienceCategoryTreeUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        ScienceCategoryTreeVO child = category(2, "生物医用无机材料", 1, 0, 0);
        ScienceCategoryTreeVO root = category(1, "生物医用材料（科学）", 0, 0, 30);
        root.setChildren(List.of(child));
        when(datasetService.getScienceCategoryTree(any()))
                .thenReturn(ScienceCategoryTreeResult.success(List.of(root), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService))
                .build();

        ScienceCategoryTreeQueryDTO query = new ScienceCategoryTreeQueryDTO();
        query.setKeyword("生物医用");
        query.setPage(1);
        query.setPageSize(50);

        mockMvc.perform(post("/api/categories/science/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].children[0].level").value(1))
                .andExpect(jsonPath("$.total").value(1));

        verify(datasetService).getScienceCategoryTree(any());
    }

    @Test
    void getProductCategoryTreeUsesDocumentedPathAndResponseShape() throws Exception {
        DatasetService datasetService = mock(DatasetService.class);
        ProductCategoryTreeVO child = productCategory("p2", "3.1", "先进钢铁材料");
        ProductCategoryTreeVO root = productCategory("p1", "3", "新材料产业");
        root.setChildren(List.of(child));
        when(datasetService.getProductCategoryTree(any()))
                .thenReturn(ProductCategoryTreeResult.success(List.of(root), 8));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ManuDatasetController(datasetService))
                .build();

        ProductCategoryTreeQueryDTO query = new ProductCategoryTreeQueryDTO();
        query.setIndustryKeyword("新材料");
        query.setSectorKeyword("");
        query.setProductKeyword("");

        mockMvc.perform(post("/api/categories/product/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value("p1"))
                .andExpect(jsonPath("$.data[0].industryCode").value("3"))
                .andExpect(jsonPath("$.data[0].industryName").value("新材料产业"))
                .andExpect(jsonPath("$.data[0].children[0].id").value("p2"))
                .andExpect(jsonPath("$.total").value(8));

        verify(datasetService).getProductCategoryTree(any());
    }

    private ScienceCategoryTreeVO category(Integer id, String name, Integer level,
                                           Integer datasetCount, Integer templateCount) {
        ScienceCategoryTreeVO vo = new ScienceCategoryTreeVO();
        vo.setId(id);
        vo.setName(name);
        vo.setLevel(level);
        vo.setDatasetCount(datasetCount);
        vo.setTemplateCount(templateCount);
        return vo;
    }

    private ProductCategoryTreeVO productCategory(String id, String industryCode, String industryName) {
        ProductCategoryTreeVO vo = new ProductCategoryTreeVO();
        vo.setId(id);
        vo.setIndustryCode(industryCode);
        vo.setIndustryName(industryName);
        vo.setSectorCode("");
        vo.setSectorName("");
        vo.setProductCode("");
        vo.setProductName("");
        return vo;
    }
}
