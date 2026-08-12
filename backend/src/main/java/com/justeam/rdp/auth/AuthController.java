package com.justeam.rdp.auth;

import com.justeam.rdp.common.ApiResponse;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import com.justeam.rdp.security.RdpProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CaptchaService captchas;
    private final RdpProperties properties;
    private final RegistrationService registrations;
    private final AuthAbuseGuard abuseGuard;
    private final ClientIpResolver clientIps;

    public AuthController(AuthService auth, CaptchaService captchas, RdpProperties properties,
                          RegistrationService registrations, AuthAbuseGuard abuseGuard, ClientIpResolver clientIps) {
        this.auth = auth;
        this.captchas = captchas;
        this.properties = properties;
        this.registrations = registrations;
        this.abuseGuard = abuseGuard;
        this.clientIps = clientIps;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.Captcha> captcha(HttpServletRequest request) {
        abuseGuard.captcha(clientIps.resolve(request));
        return ApiResponse.ok(captchas.create());
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginBody body, HttpServletRequest request,
                                                       HttpServletResponse response) {
        AuthService.LoginResult result=auth.login(new AuthService.LoginRequest(
                body.username(), body.password(), body.captchaKey(), body.captchaCode()), clientIps.resolve(request));
        setRefreshCookie(response,result.refreshToken(),properties.security().refreshTokenSeconds());
        return ApiResponse.ok("登录成功",result);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.LoginResult> refresh(@CookieValue(name="rdp_refresh",required=false) String refreshToken,
                                                         HttpServletResponse response) {
        if(refreshToken==null||refreshToken.isBlank()) throw com.justeam.rdp.common.BusinessException.unauthorized("刷新令牌不存在");
        AuthService.LoginResult result=auth.refresh(refreshToken);
        setRefreshCookie(response,result.refreshToken(),properties.security().refreshTokenSeconds());
        return ApiResponse.ok(result);
    }

    @GetMapping("/registration-settings")
    public ApiResponse<Map<String,Object>> registrationSettings() {
        return ApiResponse.ok(registrations.settings());
    }

    @PostMapping("/registration-code")
    public ApiResponse<RegistrationService.CodeDispatch> registrationCode(@Valid @RequestBody RegistrationCodeBody body,
                                                                           HttpServletRequest request) {
        return ApiResponse.ok("验证码已发送", registrations.requestCode(new RegistrationService.CodeRequest(
                body.channel(), body.target(), body.captchaKey(), body.captchaCode()), clientIps.resolve(request)));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterBody body) {
        long applicationId = registrations.register(new RegistrationService.RegisterRequest(body.username(), body.realName(),
                body.email(), body.phone(), body.password(), body.confirmPassword(), body.verificationChannel(),
                body.verificationId(), body.verificationCode(), body.captchaKey(), body.captchaCode()));
        return ApiResponse.ok("注册申请已提交，请等待管理员审核", Map.of("applicationId", applicationId, "status", "PENDING"));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        auth.logout(CurrentUser.require().id());
        setRefreshCookie(response,"",0);
        return ApiResponse.ok("已安全退出", null);
    }


    private void setRefreshCookie(HttpServletResponse response,String value,long maxAge){
        ResponseCookie cookie=ResponseCookie.from("rdp_refresh",value).httpOnly(true).secure(properties.security().secureCookies())
                .sameSite("Strict").path("/api/auth").maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }

    public record LoginBody(@NotBlank(message = "请输入用户名") @Size(max=50, message="用户名过长") String username,
                            @NotBlank(message = "请输入密码") @Size(max=128, message="密码过长") String password,
                            @NotBlank(message = "验证码标识不能为空") @Pattern(regexp="^[0-9a-fA-F-]{36}$", message="验证码标识不正确") String captchaKey,
                            @NotBlank(message = "请输入验证码") @Pattern(regexp="^\\d{1,2}$", message="验证码格式不正确") String captchaCode) {}

    public record RegisterBody(
            @NotBlank(message = "请输入用户名") @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$", message = "用户名需为3-50位字母、数字或下划线") String username,
            @NotBlank(message = "请输入真实姓名") @Size(min = 2, max = 100, message = "真实姓名需为2-100字符") String realName,
            @Email(message = "邮箱格式不正确") @Size(max=100, message="邮箱过长") String email,
            @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank(message = "请输入密码") @Size(max=128, message="密码过长") String password,
            @NotBlank(message = "请确认密码") @Size(max=128, message="确认密码过长") String confirmPassword,
            @NotBlank(message = "请选择验证码渠道") @Pattern(regexp = "^(EMAIL|PHONE)$", message = "验证码渠道不正确") String verificationChannel,
            @NotBlank(message = "请先获取验证码") @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "验证码标识不正确") String verificationId,
            @NotBlank(message = "请输入6位验证码") @Pattern(regexp = "^\\d{6}$", message = "验证码需为6位数字") String verificationCode,
            @NotBlank(message = "验证码标识不能为空") @Pattern(regexp="^[0-9a-fA-F-]{36}$", message="验证码标识不正确") String captchaKey,
            @NotBlank(message = "请输入验证码") @Pattern(regexp="^\\d{1,2}$", message="验证码格式不正确") String captchaCode) {}

    public record RegistrationCodeBody(
            @NotBlank(message = "请选择验证码渠道") @Pattern(regexp = "^(EMAIL|PHONE)$", message = "验证码渠道不正确") String channel,
            @NotBlank(message = "请输入邮箱或手机号") @Size(max=100, message = "验证码接收目标过长") String target,
            @NotBlank(message = "验证码标识不能为空") @Pattern(regexp="^[0-9a-fA-F-]{36}$", message="验证码标识不正确") String captchaKey,
            @NotBlank(message = "请输入图形验证码") @Pattern(regexp="^\\d{1,2}$", message="验证码格式不正确") String captchaCode) {}

}
