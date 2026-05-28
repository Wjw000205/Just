package org.example.just.dto.datasetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "OnlineFormSchemaVO", description = "在线填写表单结构")
public class OnlineFormSchemaVO {

    @Schema(description = "表单分区数组")
    private List<OnlineFormSectionVO> sections = new ArrayList<>();
}
