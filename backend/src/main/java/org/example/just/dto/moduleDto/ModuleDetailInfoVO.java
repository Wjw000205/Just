package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "模板具体字段信息")
public class ModuleDetailInfoVO {

    @Schema(description = "模板id")
    private Integer moduleId;

    @Schema(description = "Object字段列表")
    private List<ModuleColumnItemVO> object = new ArrayList<>();

    @Schema(description = "Operation字段列表")
    private List<ModuleColumnItemVO> operation = new ArrayList<>();

    @Schema(description = "Result字段列表")
    private List<ModuleColumnItemVO> result = new ArrayList<>();
}