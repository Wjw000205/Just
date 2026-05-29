package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OnlineFormSubmitErrorVO", description = "在线填写提交错误")
public class OnlineFormSubmitErrorVO {

    @Schema(description = "记录下标，从0开始", example = "1")
    private Integer rowIndex;

    @Schema(description = "字段标识", example = "sinterTemp")
    private String field;

    @Schema(description = "错误信息", example = "烧结温度不能为空")
    private String message;
}
