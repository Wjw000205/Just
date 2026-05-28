package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "FavoriteRequest", description = "收藏请求")
public class FavoriteRequest {

    @Schema(description = "是否收藏")
    private Boolean favorited;
}
