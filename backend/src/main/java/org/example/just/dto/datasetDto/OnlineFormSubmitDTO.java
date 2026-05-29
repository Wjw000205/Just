package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(name = "OnlineFormSubmitDTO", description = "在线填写提交数据请求")
public class OnlineFormSubmitDTO {

    @Schema(description = "数据集ID", example = "123")
    private Integer datasetId;

    @Schema(description = "在线填写记录数组")
    private List<Map<String, Object>> records;
}
