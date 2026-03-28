package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.just.utils.PageResult;

import java.util.List;

@Data
@Schema(name = "ModuleDataPageVO", description = "模板数据分页返回对象")
public class DatasetDataPageVO {

    @Schema(description = "数据集名称")
    private String datasetName;

    @Schema(description = "模板列定义")
    private List<DatasetColumnVO> columns;

    @Schema(description = "分页行数据")
    private PageResult<DatasetRowVO> pageData;
}