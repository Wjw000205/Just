package org.example.just.service.imp;

import org.example.just.dao.ModuleColumnDao;
import org.example.just.dao.ModuleDao;
import org.example.just.dao.UserDao;
import org.example.just.dto.moduleDto.ModuleBaseInfoVO;
import org.example.just.entity.ModuleEntity;
import org.example.just.entity.UserEntity;
import org.example.just.utils.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleServiceImpTest {

    @Test
    void getModuleBaseInfoReturnsCreatorUsername() {
        ModuleDao moduleDao = mock(ModuleDao.class);
        ModuleColumnDao moduleColumnDao = mock(ModuleColumnDao.class);
        UserDao userDao = mock(UserDao.class);
        ModuleServiceImp service = new ModuleServiceImp(moduleDao, moduleColumnDao, userDao);

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
}
