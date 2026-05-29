package org.example.just.dto.moduleDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TemplateOptionVO", description = "template option")
public class TemplateOptionVO {

    @Schema(description = "template id")
    private Integer id;

    @Schema(description = "template name")
    private String name;
}
