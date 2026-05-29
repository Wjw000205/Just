package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TemplateTagVO", description = "template tag option")
public class TemplateTagVO {

    @Schema(description = "tag id")
    private Integer id;

    @Schema(description = "tag name")
    private String name;
}
