package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "MyDatasetsResult", description = "我创建的数据集列表响应")
public class MyDatasetsResult {

    @Schema(description = "业务状态码，0表示成功", example = "0")
    private Integer code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "数据集列表")
    private List<ManuDatasetTreeVO> data;
}
