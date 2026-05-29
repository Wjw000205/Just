package org.example.just.controller;

import org.example.just.dto.moduleDto.TemplateOptionVO;
import org.example.just.dto.moduleDto.TemplateTagVO;
import org.example.just.service.ModuleService;
import org.example.just.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModuleControllerTest {

    @Test
    void getTemplateTagsUsesModuleControllerAndDocumentedPath() throws Exception {
        ModuleService moduleService = mock(ModuleService.class);
        when(moduleService.getTemplateTags())
                .thenReturn(Result.success(0, "success", List.of(
                        new TemplateTagVO(11, "tag-a"),
                        new TemplateTagVO(12, "tag-b")
                )));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ModuleController(moduleService))
                .build();

        mockMvc.perform(get("/api/dicts/template-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].name").value("tag-a"))
                .andExpect(jsonPath("$.data[1].id").value(12))
                .andExpect(jsonPath("$.data[1].name").value("tag-b"));

        verify(moduleService).getTemplateTags();
    }

    @Test
    void getTemplatesUsesDocumentedPathAndForwardsOptionalFilters() throws Exception {
        ModuleService moduleService = mock(ModuleService.class);
        when(moduleService.getTemplateOptions(11, 1001))
                .thenReturn(Result.success(0, "success", List.of(
                        new TemplateOptionVO(5, "template-a"),
                        new TemplateOptionVO(6, "template-b")
                )));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ModuleController(moduleService))
                .build();

        mockMvc.perform(get("/api/dicts/templates")
                        .param("templateTagId", "11")
                        .param("scienceCategoryId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].name").value("template-a"))
                .andExpect(jsonPath("$.data[1].id").value(6))
                .andExpect(jsonPath("$.data[1].name").value("template-b"));

        verify(moduleService).getTemplateOptions(11, 1001);
    }
}
