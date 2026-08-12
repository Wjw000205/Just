package com.justeam.rdp.governance;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.StepUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {
    private final GovernanceService service;private final StepUpService stepUp;
    public GovernanceController(GovernanceService service,StepUpService stepUp){this.service=service;this.stepUp=stepUp;}

    @GetMapping("/navigation") @PreAuthorize("isAuthenticated()") public ApiResponse<List<Map<String,Object>>> navigation(){return ApiResponse.ok(service.navigation());}
    @GetMapping("/options") @PreAuthorize("isAuthenticated()") public ApiResponse<Map<String,Object>> businessOptions(){return ApiResponse.ok(service.businessOptions());}
    @GetMapping("/overview") @PreAuthorize("hasAuthority('governance:read')") public ApiResponse<Map<String,Object>> overview(){return ApiResponse.ok(service.overview());}
    @GetMapping("/ui-config") @PreAuthorize("isAuthenticated()") public ApiResponse<Map<String,Object>> uiConfig(){return ApiResponse.ok(service.uiConfig());}
    @PutMapping("/ui-config") @PreAuthorize("hasAuthority('ui:manage')") public ApiResponse<Void> updateUiConfig(@Valid @RequestBody UiConfigBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateUiConfig(body.primaryColor(),body.fontSize(),body.borderRadius(),body.contentSpacing(),body.version());return ApiResponse.ok("界面配置已更新",null);}

    @GetMapping("/departments") @PreAuthorize("hasAnyAuthority('organization:manage','user:manage')") public ApiResponse<List<Map<String,Object>>> departments(){return ApiResponse.ok(service.departments());}
    @GetMapping("/department-leaders") @PreAuthorize("hasAuthority('organization:manage')") public ApiResponse<List<Map<String,Object>>> departmentLeaders(){return ApiResponse.ok(service.departmentLeaders());}
    @PostMapping("/departments") @PreAuthorize("hasAuthority('organization:manage')") public ApiResponse<Map<String,Long>> createDepartment(@Valid @RequestBody DepartmentBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("部门已创建",service.createDepartment(body.name(),body.code(),body.parentId(),body.leaderId(),body.sortOrder(),body.status()));}
    @PutMapping("/departments/{id}") @PreAuthorize("hasAuthority('organization:manage')") public ApiResponse<Void> updateDepartment(@PathVariable long id,@Valid @RequestBody DepartmentBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateDepartment(id,body.name(),body.code(),body.parentId(),body.leaderId(),body.sortOrder(),body.status());return ApiResponse.ok("部门已更新",null);}
    @DeleteMapping("/departments/{id}") @PreAuthorize("hasAuthority('organization:manage')") public ApiResponse<Void> deleteDepartment(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteDepartment(id,body.reason());return ApiResponse.ok("部门已删除",null);}

    @GetMapping("/permissions") @PreAuthorize("hasAuthority('role:manage')") public ApiResponse<List<Map<String,Object>>> permissions(){return ApiResponse.ok(service.permissions());}
    @GetMapping("/roles") @PreAuthorize("hasAuthority('role:manage')") public ApiResponse<List<Map<String,Object>>> roles(){return ApiResponse.ok(service.roles());}
    @PostMapping("/roles") @PreAuthorize("hasAuthority('role:manage') and hasAuthority('permission:assign')") public ApiResponse<Map<String,Long>> createRole(@Valid @RequestBody RoleBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("角色已创建",service.createRole(body.name(),body.code(),body.description(),body.sortOrder(),body.status(),body.permissionIds()));}
    @PutMapping("/roles/{id}") @PreAuthorize("hasAuthority('role:manage') and hasAuthority('permission:assign')") public ApiResponse<Void> updateRole(@PathVariable long id,@Valid @RequestBody RoleUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateRole(id,body.name(),body.description(),body.sortOrder(),body.status(),body.permissionIds(),body.version());return ApiResponse.ok("角色和权限已更新",null);}
    @DeleteMapping("/roles/{id}") @PreAuthorize("hasAuthority('role:manage')") public ApiResponse<Void> deleteRole(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteRole(id,body.reason());return ApiResponse.ok("角色已删除",null);}

    @GetMapping("/data-scopes") @PreAuthorize("hasAuthority('data-scope:manage')") public ApiResponse<List<Map<String,Object>>> dataScopes(){return ApiResponse.ok(service.dataScopes());}
    @PostMapping("/data-scopes") @PreAuthorize("hasAuthority('data-scope:manage')") public ApiResponse<Map<String,Long>> createDataScope(@Valid @RequestBody DataScopeBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("数据域已创建",service.createDataScope(body.name(),body.code(),body.type(),body.description(),body.active()));}
    @PutMapping("/data-scopes/{id}") @PreAuthorize("hasAuthority('data-scope:manage')") public ApiResponse<Void> updateDataScope(@PathVariable long id,@Valid @RequestBody DataScopeUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateDataScope(id,body.name(),body.type(),body.description(),body.active(),body.version());return ApiResponse.ok("数据域已更新",null);}
    @DeleteMapping("/data-scopes/{id}") @PreAuthorize("hasAuthority('data-scope:manage')") public ApiResponse<Void> deleteDataScope(@PathVariable long id,@Valid @RequestBody VersionedDeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteDataScope(id,body.reason(),body.version());return ApiResponse.ok("数据域已删除",null);}

    @GetMapping("/categories") @PreAuthorize("hasAuthority('governance:read')") public ApiResponse<List<Map<String,Object>>> categories(@RequestParam(required=false)String type){return ApiResponse.ok(service.categories(type));}
    @PostMapping("/categories") @PreAuthorize("hasAuthority('classification:manage')") public ApiResponse<Map<String,Long>> createCategory(@Valid @RequestBody CategoryBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("分类已创建",service.createCategory(body.type(),body.name(),body.code(),body.parentId(),body.productCodes(),body.sortOrder(),body.status()));}
    @PutMapping("/categories/{id}") @PreAuthorize("hasAuthority('classification:manage')") public ApiResponse<Void> updateCategory(@PathVariable long id,@Valid @RequestBody CategoryUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateCategory(id,body.name(),body.code(),body.parentId(),body.productCodes(),body.sortOrder(),body.status());return ApiResponse.ok("分类已更新",null);}
    @DeleteMapping("/categories/{id}") @PreAuthorize("hasAuthority('classification:manage')") public ApiResponse<Void> deleteCategory(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteCategory(id,body.reason());return ApiResponse.ok("分类已删除",null);}
    @PutMapping("/category-mappings/{scientificId}") @PreAuthorize("hasAuthority('classification:manage')") public ApiResponse<Void> replaceMappings(@PathVariable long scientificId,@Valid @RequestBody MappingBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.replaceCategoryMappings(scientificId,body.industryCategoryIds());return ApiResponse.ok("分类映射已更新",null);}

    @GetMapping("/tags") @PreAuthorize("hasAuthority('governance:read')") public ApiResponse<List<Map<String,Object>>> tags(){return ApiResponse.ok(service.tags());}
    @PostMapping("/tags") @PreAuthorize("hasAuthority('tag:manage')") public ApiResponse<Map<String,Long>> createTag(@Valid @RequestBody TagBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("标签已创建",service.createTag(body.name(),body.color(),body.group(),body.status()));}
    @PutMapping("/tags/{id}") @PreAuthorize("hasAuthority('tag:manage')") public ApiResponse<Void> updateTag(@PathVariable long id,@Valid @RequestBody TagBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateTag(id,body.name(),body.color(),body.group(),body.status());return ApiResponse.ok("标签已更新",null);}
    @DeleteMapping("/tags/{id}") @PreAuthorize("hasAuthority('tag:manage')") public ApiResponse<Void> deleteTag(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteTag(id,body.reason());return ApiResponse.ok("标签已删除",null);}

    @GetMapping("/dictionaries") @PreAuthorize("hasAuthority('governance:read')") public ApiResponse<List<Map<String,Object>>> dictionaries(){return ApiResponse.ok(service.dictionaries());}
    @PostMapping("/dictionaries") @PreAuthorize("hasAuthority('dictionary:manage')") public ApiResponse<Map<String,Long>> createDictionary(@Valid @RequestBody DictionaryBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("字典已创建",service.createDictionary(body.name(),body.code(),body.description(),body.status()));}
    @PutMapping("/dictionaries/{id}") @PreAuthorize("hasAuthority('dictionary:manage')") public ApiResponse<Void> updateDictionary(@PathVariable long id,@Valid @RequestBody DictionaryUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateDictionary(id,body.name(),body.description(),body.status());return ApiResponse.ok("字典已更新",null);}
    @DeleteMapping("/dictionaries/{id}") @PreAuthorize("hasAuthority('dictionary:manage')") public ApiResponse<Void> deleteDictionary(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteDictionary(id,body.reason());return ApiResponse.ok("字典已删除",null);}
    @GetMapping("/dictionary-items") @PreAuthorize("hasAuthority('governance:read')") public ApiResponse<List<Map<String,Object>>> dictionaryItems(@RequestParam(required=false)Long dictionaryId){return ApiResponse.ok(service.dictionaryItems(dictionaryId));}
    @PostMapping("/dictionary-items") @PreAuthorize("hasAuthority('dictionary:manage')") public ApiResponse<Map<String,Long>> createDictionaryItem(@Valid @RequestBody DictionaryItemBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("字典项已创建",service.createDictionaryItem(body.dictionaryId(),body.label(),body.value(),body.sortOrder(),body.status()));}
    @PutMapping("/dictionary-items/{id}") @PreAuthorize("hasAuthority('dictionary:manage')") public ApiResponse<Void> updateDictionaryItem(@PathVariable long id,@Valid @RequestBody DictionaryItemUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateDictionaryItem(id,body.label(),body.value(),body.sortOrder(),body.status());return ApiResponse.ok("字典项已更新",null);}
    @DeleteMapping("/dictionary-items/{id}") @PreAuthorize("hasAuthority('dictionary:manage')") public ApiResponse<Void> deleteDictionaryItem(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteDictionaryItem(id,body.reason());return ApiResponse.ok("字典项已删除",null);}

    @GetMapping("/menus") @PreAuthorize("hasAuthority('governance:read')") public ApiResponse<List<Map<String,Object>>> menus(){return ApiResponse.ok(service.menus());}
    @PostMapping("/menus") @PreAuthorize("hasAuthority('menu:manage')") public ApiResponse<Map<String,Long>> createMenu(@Valid @RequestBody MenuBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);return created("菜单已创建",service.createMenu(body.name(),body.code(),body.route(),body.icon(),body.permissionCode(),body.parentId(),body.sortOrder(),body.status()));}
    @PutMapping("/menus/{id}") @PreAuthorize("hasAuthority('menu:manage')") public ApiResponse<Void> updateMenu(@PathVariable long id,@Valid @RequestBody MenuUpdateBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.updateMenu(id,body.name(),body.route(),body.icon(),body.permissionCode(),body.parentId(),body.sortOrder(),body.status());return ApiResponse.ok("菜单已更新",null);}
    @DeleteMapping("/menus/{id}") @PreAuthorize("hasAuthority('menu:manage')") public ApiResponse<Void> deleteMenu(@PathVariable long id,@Valid @RequestBody DeleteBody body,@RequestHeader(value=StepUpService.HEADER,required=false)String proof,HttpServletRequest request){verify(proof,request,body);service.deleteMenu(id,body.reason());return ApiResponse.ok("菜单已删除",null);}

    private void verify(String proof,HttpServletRequest request,Object body){stepUp.consume(CurrentUser.require(),proof,StepUpService.Purpose.PRIVILEGE,request,body);}
    private ApiResponse<Map<String,Long>> created(String message,long id){return ApiResponse.ok(message,Map.of("id",id));}

    public record DeleteBody(@NotBlank @Size(max=1000)String reason){}
    public record VersionedDeleteBody(@NotBlank @Size(max=1000)String reason,@Positive int version){}
    public record DataScopeBody(@NotBlank @Size(max=120)String name,@NotBlank @Size(max=80)String code,@NotBlank @Pattern(regexp="GLOBAL|DEPARTMENT|PROJECT|PRODUCT_LINE")String type,@Size(max=500)String description,boolean active){}
    public record DataScopeUpdateBody(@NotBlank @Size(max=120)String name,@NotBlank @Pattern(regexp="GLOBAL|DEPARTMENT|PROJECT|PRODUCT_LINE")String type,@Size(max=500)String description,boolean active,@Positive int version){}
    public record DepartmentBody(@NotBlank @Size(max=100)String name,@NotBlank @Size(max=50)String code,@Positive Long parentId,@Positive Long leaderId,int sortOrder,@Min(0)@Max(1)int status){}
    public record RoleBody(@NotBlank @Size(max=50)String name,@NotBlank @Size(max=50)String code,@Size(max=255)String description,int sortOrder,@Min(0)@Max(1)int status,List<@Positive Long> permissionIds){}
    public record RoleUpdateBody(@NotBlank @Size(max=50)String name,@Size(max=255)String description,int sortOrder,@Min(0)@Max(1)int status,List<@Positive Long> permissionIds,@Positive int version){}
    public record CategoryBody(@NotBlank @Pattern(regexp="SCIENTIFIC|INDUSTRY")String type,@NotBlank @Size(max=100)String name,@NotBlank @Size(max=50)String code,@Positive Long parentId,List<@Size(max=80)String> productCodes,int sortOrder,@Min(0)@Max(1)int status){}
    public record CategoryUpdateBody(@NotBlank @Size(max=100)String name,@NotBlank @Size(max=50)String code,@Positive Long parentId,List<@Size(max=80)String> productCodes,int sortOrder,@Min(0)@Max(1)int status){}
    public record MappingBody(List<@Positive Long> industryCategoryIds){}
    public record TagBody(@NotBlank @Size(max=80)String name,@NotBlank @Pattern(regexp="#[0-9A-Fa-f]{6}")String color,@NotBlank @Size(max=80)String group,@Min(0)@Max(1)int status){}
    public record DictionaryBody(@NotBlank @Size(max=100)String name,@NotBlank @Size(max=80)String code,@Size(max=500)String description,@Min(0)@Max(1)int status){}
    public record DictionaryUpdateBody(@NotBlank @Size(max=100)String name,@Size(max=500)String description,@Min(0)@Max(1)int status){}
    public record DictionaryItemBody(@Positive long dictionaryId,@NotBlank @Size(max=100)String label,@NotBlank @Size(max=200)String value,int sortOrder,@Min(0)@Max(1)int status){}
    public record DictionaryItemUpdateBody(@NotBlank @Size(max=100)String label,@NotBlank @Size(max=200)String value,int sortOrder,@Min(0)@Max(1)int status){}
    public record MenuBody(@NotBlank @Size(max=80)String name,@NotBlank @Size(max=80)String code,@Size(max=200)String route,@Size(max=40)String icon,@Size(max=100)String permissionCode,@Positive Long parentId,int sortOrder,@Min(0)@Max(1)int status){}
    public record MenuUpdateBody(@NotBlank @Size(max=80)String name,@Size(max=200)String route,@Size(max=40)String icon,@Size(max=100)String permissionCode,@Positive Long parentId,int sortOrder,@Min(0)@Max(1)int status){}
    public record UiConfigBody(@NotBlank @Pattern(regexp="#[0-9A-Fa-f]{6}")String primaryColor,@Min(12)@Max(20)int fontSize,@Min(0)@Max(24)int borderRadius,@Min(8)@Max(40)int contentSpacing,@Positive int version){}
}
