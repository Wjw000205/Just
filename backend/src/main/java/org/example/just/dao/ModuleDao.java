package org.example.just.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.just.entity.ModuleEntity;

import java.util.List;

@Mapper
public interface ModuleDao extends BaseMapper<ModuleEntity> {

    @Select({
            "SELECT *",
            "FROM `module`",
            "WHERE deleted = 0",
            "AND audit_state = 2",
            "ORDER BY id ASC"
    })
    List<ModuleEntity> selectAvailableTemplates();

    @Select({
            "SELECT DISTINCT m.*",
            "FROM `module` m",
            "INNER JOIN manu_dataset d ON d.`module` = m.id",
            "WHERE m.deleted = 0",
            "AND m.audit_state = 2",
            "AND d.deleted = 0",
            "AND d.is_menu = 0",
            "AND d.science_category_id = #{scienceCategoryId}",
            "ORDER BY m.id ASC"
    })
    List<ModuleEntity> selectAvailableTemplatesByScienceCategory(@Param("scienceCategoryId") Integer scienceCategoryId);
}
