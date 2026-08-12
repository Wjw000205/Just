package com.justeam.rdp.admin;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.PageResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import com.justeam.rdp.auth.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;
    private final UserAdministrationService users;
    private final StepUpService stepUp;
    private final RegistrationService registrations;
    public AdminController(AdminService service,UserAdministrationService users,StepUpService stepUp,
                           RegistrationService registrations){this.service=service;this.users=users;this.stepUp=stepUp;this.registrations=registrations;}

    @GetMapping("/users") @PreAuthorize("hasAnyAuthority('user:read','user:manage')")
    public ApiResponse<PageResponse<Map<String,Object>>> users(@RequestParam(required=false)String keyword,
            @RequestParam(required=false)Integer status,@RequestParam(required=false)Long departmentId,@RequestParam(defaultValue="1")int pageNum,
            @RequestParam(defaultValue="20")int pageSize){boolean manage=CurrentUser.require().permissions().contains("user:manage");return ApiResponse.ok(service.users(keyword,status,departmentId,pageNum,pageSize,manage));}

    @GetMapping("/options") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Map<String,Object>> options(){return ApiResponse.ok(service.options());}

    @GetMapping("/registrations") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<PageResponse<Map<String,Object>>> registrations(@RequestParam(required=false)String status,
            @RequestParam(required=false)String keyword,@RequestParam(defaultValue="1")int pageNum,
            @RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(registrations.applications(status,keyword,pageNum,pageSize));}

    @PostMapping("/registrations/{id}/review")
    @PreAuthorize("hasAuthority('user:manage') and hasAuthority('permission:assign')")
    public ApiResponse<Void> reviewRegistration(@PathVariable long id,@Valid @RequestBody RegistrationReviewBody body,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);
        registrations.review(id,new RegistrationService.ReviewRequest(body.decision(),body.comment(),body.roleIds(),
                body.scopeIds(),body.departmentId(),body.applicationVersion(),body.authorizationVersion()));
        return ApiResponse.ok("注册申请审核完成",null);
    }

    @PutMapping("/users/{id}/status") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Void> status(@PathVariable long id,@RequestBody @Valid StatusBody body,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);
        service.setStatus(id,body.status(),body.reason());return ApiResponse.ok("状态已更新",null);
    }

    @PutMapping("/users/{id}/assignments") @PreAuthorize("hasAuthority('user:manage') and hasAuthority('permission:assign')")
    public ApiResponse<Void> assign(@PathVariable long id,@RequestBody @Valid AssignBody body,
            @RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){
        stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);
        service.assign(id,body.roleIds(),body.scopeIds(),body.departmentId(),body.reason(),body.version());return ApiResponse.ok("授权已更新",null);
    }

    @PostMapping("/users") @PreAuthorize("hasAuthority('user:manage') and hasAuthority('permission:assign')")
    public ApiResponse<Map<String,Object>> create(@Valid @RequestBody UserCreateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);return ApiResponse.ok("用户已创建，请安全传递一次性临时密码",users.create(body.username(),body.realName(),body.email(),body.phone(),body.departmentId(),body.roleIds(),body.scopeIds(),body.status(),body.reason()));}

    @PutMapping("/users/{id}") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Void> update(@PathVariable long id,@Valid @RequestBody UserUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);users.update(id,body.realName(),body.email(),body.phone(),body.reason());return ApiResponse.ok("用户资料已更新；部门请通过授权接口调整",null);}

    @PostMapping("/users/{id}/reset-password") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Map<String,String>> resetPassword(@PathVariable long id,@Valid @RequestBody ReasonBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);return ApiResponse.ok("密码已重置，仅本次返回临时密码",users.resetPassword(id,body.reason()));}

    @DeleteMapping("/users/{id}") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Void> delete(@PathVariable long id,@Valid @RequestBody ReasonBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);users.delete(id,body.reason());return ApiResponse.ok("用户已删除",null);}

    @PostMapping(value="/users/import",consumes="multipart/form-data") @PreAuthorize("hasAuthority('user:manage') and hasAuthority('permission:assign')")
    public ApiResponse<Map<String,Object>> importUsers(@RequestPart MultipartFile file,@RequestPart String reason,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,importProof(file,reason));return ApiResponse.ok("用户批量导入完成",users.importUsers(file,reason));}

    private ImportProofBody importProof(MultipartFile file,String reason){
        if(file==null||file.isEmpty())throw BusinessException.badRequest("请选择导入文件");
        if(file.getSize()>5*1024*1024)throw new BusinessException(413,"用户导入文件不能超过5MB");
        if(reason==null||reason.isBlank())throw BusinessException.badRequest("导入原因不能为空");
        try(var input=file.getInputStream()){
            MessageDigest digest=MessageDigest.getInstance("SHA-256");input.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(),digest));
            return new ImportProofBody(reason.trim(),file.getSize(),HexFormat.of().formatHex(digest.digest()));
        }catch(Exception ex){throw BusinessException.badRequest("用户导入文件无法读取");}
    }

    public record StatusBody(@Min(0)@Max(1)int status,@NotBlank String reason){}
    public record AssignBody(@NotEmpty List<@Positive Long> roleIds,List<@Positive Long> scopeIds,
                             @Positive Long departmentId,@NotBlank String reason,@Positive int version){}
    public record UserCreateBody(@NotBlank @Size(max=50)String username,@NotBlank @Size(max=100)String realName,@Email @Size(max=100)String email,@Size(max=20)String phone,@Positive Long departmentId,@NotEmpty List<@Positive Long> roleIds,List<@Positive Long> scopeIds,@Min(0)@Max(1)int status,@NotBlank @Size(max=1000)String reason){}
    public record UserUpdateBody(@NotBlank @Size(max=100)String realName,@Email @Size(max=100)String email,@Size(max=20)String phone,@NotBlank @Size(max=1000)String reason){}
    public record ReasonBody(@NotBlank @Size(max=1000)String reason){}
    public record ImportProofBody(String reason,long size,String sha256){}
    public record RegistrationReviewBody(
            @NotBlank @jakarta.validation.constraints.Pattern(regexp="^(APPROVE|REJECT)$") String decision,
            @NotBlank @Size(max=1000) String comment,
            List<@Positive Long> roleIds,List<@Positive Long> scopeIds,@Positive Long departmentId,
            @Positive int applicationVersion,@Positive int authorizationVersion){}
}
