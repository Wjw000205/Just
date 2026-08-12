package com.justeam.rdp.sharing;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shares")
public class SharingController {
    private final SharingService service;private final StepUpService stepUp;
    public SharingController(SharingService service,StepUpService stepUp){this.service=service;this.stepUp=stepUp;}
    @GetMapping @PreAuthorize("hasAuthority('share:read')") public ApiResponse<PageResponse<Map<String,Object>>> list(@RequestParam(required=false)String status,@RequestParam(required=false)String resourceType,@RequestParam(defaultValue="1")int pageNum,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(service.list(status,resourceType,pageNum,pageSize));}
    @GetMapping("/mine") @PreAuthorize("isAuthenticated()") public ApiResponse<List<Map<String,Object>>> mine(){return ApiResponse.ok(service.mine());}
    @GetMapping("/options") @PreAuthorize("hasAuthority('share:manage')") public ApiResponse<Map<String,Object>> options(){return ApiResponse.ok(service.options());}
    @PostMapping @PreAuthorize("hasAuthority('share:manage')") public ApiResponse<Map<String,Long>> create(@Valid @RequestBody CreateBody body,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);
        return ApiResponse.ok("共享规则已创建",Map.of("id",service.create(body.resourceType(),body.resourceId(),body.granteeType(),body.granteeId(),body.operations(),body.validFrom(),body.validTo(),body.reason())));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('share:revoke')") public ApiResponse<Void> revoke(@PathVariable long id,@Valid @RequestBody RevokeBody body,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);
        service.revoke(id,body.version(),body.reason());return ApiResponse.ok("共享规则已撤销",null);}
    public record CreateBody(@NotBlank @Pattern(regexp="DATA_SCOPE|DATASET")String resourceType,@Positive long resourceId,@NotBlank @Pattern(regexp="USER|ROLE|DEPARTMENT")String granteeType,@Positive long granteeId,@NotEmpty List<@Pattern(regexp="READ|EXPORT|DOWNLOAD") String> operations,Instant validFrom,@Future Instant validTo,@NotBlank @Size(max=1000)String reason){}
    public record RevokeBody(@Positive int version,@NotBlank @Size(max=1000)String reason){}
}
