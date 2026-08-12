package com.justeam.rdp.user;

import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.SecondaryCredentialService;
import com.justeam.rdp.security.StepUpService;
import com.justeam.rdp.security.TokenService;
import com.justeam.rdp.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final JdbcClient jdbc;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final AuditService audit;
    private final SecondaryCredentialService secondaryCredentials;
    private final StepUpService stepUp;

    public UserController(JdbcClient jdbc, PasswordEncoder passwords, TokenService tokens, AuditService audit,
                          SecondaryCredentialService secondaryCredentials,StepUpService stepUp) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.tokens = tokens;
        this.audit = audit;
        this.secondaryCredentials = secondaryCredentials;
        this.stepUp=stepUp;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        UserPrincipal user = CurrentUser.require();
        Map<String, Object> profile = jdbc.sql("""
                SELECT u.id, u.username, u.real_name, u.email, u.phone, u.status, u.secondary_password,
                       u.created_time, u.last_login_time, u.must_change_password, d.id AS department_id, d.dept_name AS department
                FROM sys_user u LEFT JOIN sys_department d ON d.id = u.department_id
                WHERE u.id = :id
                """).param("id", user.id()).query((rs, row) -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", rs.getLong("id"));
            value.put("username", rs.getString("username"));
            value.put("realName", rs.getString("real_name"));
            value.put("email", rs.getString("email"));
            value.put("phone", rs.getString("phone"));
            value.put("status", rs.getInt("status"));
            value.put("createdTime", rs.getObject("created_time"));
            value.put("lastLoginTime", rs.getObject("last_login_time"));
            value.put("mustChangePassword",rs.getBoolean("must_change_password"));
            value.put("secondaryPasswordSet",rs.getString("secondary_password")!=null);
            value.put("departmentId", rs.getObject("department_id"));
            value.put("department", rs.getString("department"));
            value.put("roles", user.roles());
            value.put("permissions", user.permissions());
            value.put("assignedScopes", user.assignedScopes());
            value.put("dataScopes", user.dataScopes());
            return value;
        }).single();
        return ApiResponse.ok(profile);
    }

    @PutMapping("/profile")
    @Transactional
    public ApiResponse<Void> updateProfile(@Valid @RequestBody ProfileBody body) {
        long id = CurrentUser.require().id();
        Map<String,Object> before=editableProfile(id);
        long duplicate = jdbc.sql("""
                SELECT count(*) FROM sys_user WHERE id <> :id AND deleted = 0
                AND ((:email IS NOT NULL AND email = :email) OR (:phone IS NOT NULL AND phone = :phone))
                """).param("id", id).param("email", body.email()).param("phone", body.phone())
                .query(Long.class).single();
        if (duplicate > 0) throw new BusinessException(409, "邮箱或手机号已被其他用户使用");
        jdbc.sql("""
                UPDATE sys_user SET real_name=:realName, email=:email, phone=:phone,
                                    updated_by=:id, updated_time=now() WHERE id=:id
                """).param("realName", body.realName()).param("email", body.email()).param("phone", body.phone())
                .param("id", id).update();
        audit.record("UPDATE", "USER", "修改个人资料",Map.of("userId",id,"before",before,"after",editableProfile(id)));
        return ApiResponse.ok("资料已更新", null);
    }

    @PutMapping("/password")
    @Transactional
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordBody body) {
        if (!body.newPassword().equals(body.confirmPassword())) throw BusinessException.badRequest("两次输入的新密码不一致");
        if (!body.newPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$")) {
            throw BusinessException.badRequest("新密码需为8-20位并包含大小写字母和数字");
        }
        UserPrincipal user = CurrentUser.require();
        String currentHash = jdbc.sql("SELECT password FROM sys_user WHERE id=:id").param("id", user.id())
                .query(String.class).single();
        if (!passwords.matches(body.currentPassword(), currentHash)) throw BusinessException.badRequest("当前密码不正确");
        if (passwords.matches(body.newPassword(), currentHash)) throw BusinessException.badRequest("新密码不能与当前密码相同");
        jdbc.sql("UPDATE sys_user SET password=:password, password_changed_time=now(), must_change_password=FALSE, updated_time=now() WHERE id=:id")
                .param("password", passwords.encode(body.newPassword())).param("id", user.id()).update();
        audit.record("CHANGE_PASSWORD", "USER", "修改登录密码并撤销现有会话");
        tokens.revoke(user.id());
        return ApiResponse.ok("密码已修改，请重新登录", null);
    }

    @PutMapping("/secondary-password")
    @Transactional
    public ApiResponse<Void> changeSecondaryPassword(@Valid @RequestBody SecondaryPasswordBody body) {
        if (!body.newSecondaryPassword().equals(body.confirmSecondaryPassword()))
            throw BusinessException.badRequest("两次输入的新二级密码不一致");
        UserPrincipal user=CurrentUser.require();
        CredentialHashes hashes=jdbc.sql("SELECT password,coalesce(secondary_password,'') secondary_password FROM sys_user WHERE id=:id AND status=1 AND deleted=0")
                .param("id",user.id()).query((rs,n)->new CredentialHashes(rs.getString("password"),rs.getString("secondary_password")))
                .optional().orElseThrow(()->BusinessException.unauthorized("账号不可用"));
        String currentHash=hashes.secondaryPassword().isBlank()?null:hashes.secondaryPassword();
        if(currentHash==null)secondaryCredentials.verifyLoginPasswordForSetup(user,body.currentPassword(),hashes.loginPassword());
        if(currentHash!=null)secondaryCredentials.verify(user,body.currentSecondaryPassword(),currentHash,"CHANGE_SECONDARY_PASSWORD");
        if(currentHash!=null&&passwords.matches(body.newSecondaryPassword(),currentHash))
            throw BusinessException.badRequest("新二级密码不能与当前二级密码相同");
        jdbc.sql("UPDATE sys_user SET secondary_password=:password,updated_by=:id,updated_time=now() WHERE id=:id")
                .param("password",passwords.encode(body.newSecondaryPassword())).param("id",user.id()).update();
        audit.record("CHANGE_SECONDARY_PASSWORD","USER",currentHash==null?"设置二级密码":"修改二级密码",Map.of("userId",user.id(),"previouslySet",currentHash!=null));
        return ApiResponse.ok(currentHash==null?"二级密码已设置":"二级密码已修改",null);
    }

    @PostMapping("/step-up")
    public ApiResponse<StepUpService.IssuedToken> issueStepUp(@Valid @RequestBody StepUpBody body){
        UserPrincipal user=CurrentUser.require();
        return ApiResponse.ok("二级确认已通过，凭证5分钟内仅可使用一次",
                stepUp.issue(user,body.purpose(),body.method(),body.target(),body.secondaryPassword(),body.payload()));
    }

    public record ProfileBody(@NotBlank(message = "真实姓名不能为空") @Size(min = 2, max = 100, message = "真实姓名需为2-100字符") String realName,
                              @Email(message = "邮箱格式不正确") String email,
                              @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone) {}
    public record PasswordBody(@NotBlank(message = "请输入当前密码") String currentPassword,
                               @NotBlank(message = "请输入新密码") String newPassword,
                               @NotBlank(message = "请确认新密码") String confirmPassword) {}
    public record SecondaryPasswordBody(@Size(max=128,message="当前登录密码长度不正确") String currentPassword,
                                        @Pattern(regexp="^\\d{6}$",message="当前二级密码须为6位数字") String currentSecondaryPassword,
                                        @NotBlank(message="请输入新二级密码") @Pattern(regexp="^\\d{6}$",message="新二级密码须为6位数字") String newSecondaryPassword,
                                        @NotBlank(message="请确认新二级密码") @Pattern(regexp="^\\d{6}$",message="确认二级密码须为6位数字") String confirmSecondaryPassword) {}
    public record StepUpBody(@NotBlank(message="二级确认用途不能为空") @Pattern(regexp="DELETE|EXPORT|PRIVILEGE",message="二级确认用途不正确") String purpose,
                             @NotBlank(message="请求方法不能为空") @Pattern(regexp="GET|POST|PUT|PATCH|DELETE",message="请求方法不正确") String method,
                             @NotBlank(message="敏感操作目标不能为空") @Size(max=4096,message="敏感操作目标过长") String target,
                             @NotBlank(message="请输入二级密码") @Pattern(regexp="^\\d{6}$",message="二级密码须为6位数字") String secondaryPassword,
                             Object payload){}
    private record CredentialHashes(String loginPassword,String secondaryPassword) {}
    private Map<String,Object> editableProfile(long id){return jdbc.sql("SELECT real_name,email,phone FROM sys_user WHERE id=:id AND deleted=0")
            .param("id",id).query((rs,n)->{Map<String,Object> value=new LinkedHashMap<>();value.put("realName",rs.getString("real_name"));value.put("email",rs.getString("email"));value.put("phone",rs.getString("phone"));return value;})
            .optional().orElseThrow(()->BusinessException.notFound("用户不存在"));}
}
