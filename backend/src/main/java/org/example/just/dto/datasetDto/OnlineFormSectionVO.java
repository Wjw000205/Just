package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "OnlineFormSectionVO", description = "在线填写表单分区")
public class OnlineFormSectionVO {

    @Schema(description = "分区唯一标识", example = "object")
    private String id;

    @Schema(description = "分区标题", example = "对象区域")
    private String title;

    @Schema(description = "分区副标题或说明")
    private String subtitle;

    @Schema(description = "分区字段列表")
    private List<OnlineFormFieldVO> fields = new ArrayList<>();
}
