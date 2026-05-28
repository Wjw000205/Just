package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "FavoriteResponse", description = "收藏响应")
public class FavoriteResponse {

    @Schema(description = "数据集 ID")
    private Integer id;

    @Schema(description = "是否已收藏")
    private Boolean favorited;
}
