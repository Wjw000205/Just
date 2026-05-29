package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "OnlineFormSubmitResultVO", description = "在线填写提交数据响应")
public class OnlineFormSubmitResultVO {

    @Schema(description = "成功接收记录数", example = "2")
    private Integer acceptedCount = 0;

    @Schema(description = "失败记录数", example = "0")
    private Integer failedCount = 0;

    @Schema(description = "失败明细")
    private List<OnlineFormSubmitErrorVO> errors = new ArrayList<>();
}
