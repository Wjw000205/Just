package org.example.just.service.imp;

import org.example.just.dao.ModuleColumnDao;
import org.example.just.dao.ModuleDao;
import org.example.just.dao.UserDao;
import org.example.just.dto.moduleDto.ModuleBaseInfoVO;
import org.example.just.dto.moduleDto.TemplateOptionVO;
import org.example.just.dto.moduleDto.TemplateTagVO;
import org.example.just.entity.ModuleEntity;
import org.example.just.entity.UserEntity;
import org.example.just.utils.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleServiceImpTest {

    private final ModuleDao moduleDao = mock(ModuleDao.class);
    private final ModuleColumnDao moduleColumnDao = mock(ModuleColumnDao.class);
    private final UserDao userDao = mock(UserDao.class);
    private final ModuleServiceImp service = new ModuleServiceImp(moduleDao, moduleColumnDao, userDao);

    @Test
    void getModuleBaseInfoReturnsCreatorUsername() {
        ModuleEntity module = new ModuleEntity();
        module.setId(1);
        module.setModuleName("test module");
        module.setCreator(7);

        UserEntity user = new UserEntity();
        user.setId(7);
        user.setUsername("alice");

        when(moduleDao.selectOne(any())).thenReturn(module);
        when(userDao.selectById(7)).thenReturn(user);

        Result<ModuleBaseInfoVO> result = service.getModuleBaseInfo(1);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getCreator()).isEqualTo(7);
        assertThat(result.getData().getUsername()).isEqualTo("alice");
    }

    @Test
    void getTemplateTagsBuildsDistinctTagsFromModules() {
        when(moduleDao.selectAvailableTemplates()).thenReturn(List.of(
                module(11, "tag-a", "template-a", 0),
                module(12, "tag-a", "template-b", 0),
                module(13, "tag-b", "template-c", 0),
                module(14, "deleted", "template-d", 1)
        ));

        Result<List<TemplateTagVO>> result = service.getTemplateTags();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData())
                .extracting(TemplateTagVO::getId)
                .containsExactly(11, 13);
        assertThat(result.getData())
                .extracting(TemplateTagVO::getName)
                .containsExactly("tag-a", "tag-b");
    }

    @Test
    void getTemplateOptionsFiltersByTemplateTagAndScienceCategory() {
        when(moduleDao.selectAvailableTemplatesByScienceCategory(1001)).thenReturn(List.of(
                module(5, "tag-a", "template-a", 0),
                module(7, "tag-b", "template-c", 0),
                module(8, "tag-a", "deleted-template", 1)
        ));

        Result<List<TemplateOptionVO>> result = service.getTemplateOptions(5, 1001);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo(5);
        assertThat(result.getData().get(0).getName()).isEqualTo("template-a");
    }

    private ModuleEntity module(Integer id, String tag, String moduleName, Integer deleted) {
        ModuleEntity entity = new ModuleEntity();
        entity.setId(id);
        entity.setTag(tag);
        entity.setModuleName(moduleName);
        entity.setDeleted(deleted);
        entity.setAuditState(2);
        return entity;
    }

}
