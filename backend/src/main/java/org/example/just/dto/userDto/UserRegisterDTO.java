package org.example.just.dto.userDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserRegisterDTO", description = "用户注册请求参数")
public class UserRegisterDTO {

    @Schema(description = "用户名", example = "张三")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "手机号", example = "13800138000")
    private String telephone;

    @Schema(description = "邮箱", example = "zhangsan@qq.com")
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}