package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.just.service.imp.DataBaseServiceImpl;

import java.util.List;

@Data
@Schema(name = "DataBasePageInitInfoVO", description = "数据库页面初始化信息响应")
public class DataBasePageInitInfoVO {
    
    @Schema(description = "左侧目录树")
    private List<DataBaseServiceImpl.ClassificationTreeNode> tree;
}
