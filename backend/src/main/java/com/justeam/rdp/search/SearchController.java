package com.justeam.rdp.search;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService service;
    public SearchController(SearchService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<Map<String, Object>>> search(@Valid @RequestBody Body body) {
        return ApiResponse.ok(service.search(body.resourceTypes(), body.conditions(), body.from(), body.to(),
                body.pageNum(), body.pageSize(), body.sortBy(), body.sortOrder()));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String,Object>>> history(){return ApiResponse.ok(service.history());}

    @DeleteMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> clearHistory(){service.clearHistory();return ApiResponse.ok("检索历史已清空",null);}

    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String,Object>>> suggestions(
            @RequestParam(defaultValue = "") @Size(max=50) String prefix,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit){
        return ApiResponse.ok(service.suggestions(prefix,limit));
    }

    public record Body(@NotEmpty(message = "请至少选择一种资源") @Size(max = 5) List<String> resourceTypes,
                       @Size(max = 20) List<@Valid Condition> conditions,
                       Instant from, Instant to,
                       @Min(1) @Max(value = 1000, message = "为保护检索集群，页码不能超过1000；更深结果请缩小条件") int pageNum,
                       @Min(10) @Max(50) int pageSize,
                       @Pattern(regexp = "CREATED_TIME|UPDATED_TIME|NAME|TYPE|STATUS", message = "排序字段不正确") String sortBy,
                       @Pattern(regexp = "ASC|DESC", message = "排序方向不正确") String sortOrder) {}

    public record Condition(@NotBlank @Pattern(regexp = "KEYWORD|MATERIAL|PROCESS|BATCH|DEVICE|PRODUCT_MODEL|CATEGORY|TYPE|STATUS",
                                               message = "检索字段不正确") String field,
                            @NotBlank @Size(max = 200) String value,
                            @NotBlank @Pattern(regexp = "FUZZY|EXACT", message = "匹配方式不正确") String matchMode) {}
}
