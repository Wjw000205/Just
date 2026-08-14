package com.justeam.rdp.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final JdbcClient jdbc; private final PasswordEncoder passwords; private final String bootstrapPassword;
    public BootstrapAdminInitializer(JdbcClient jdbc,PasswordEncoder passwords,
            @Value("${RDP_BOOTSTRAP_ADMIN_PASSWORD:}") String bootstrapPassword){this.jdbc=jdbc;this.passwords=passwords;this.bootstrapPassword=bootstrapPassword;}
    @Override public void run(ApplicationArguments args){
        if(bootstrapPassword==null||bootstrapPassword.isBlank())return;
        if(!bootstrapPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$"))
            throw new IllegalStateException("RDP_BOOTSTRAP_ADMIN_PASSWORD须至少12位并包含大小写字母、数字和特殊字符");
        jdbc.sql("UPDATE sys_user SET password=:password,status=1,must_change_password=FALSE,bootstrap_pending=FALSE,password_changed_time=now() WHERE username='admin' AND deleted=0 AND status=0 AND must_change_password=TRUE AND bootstrap_pending=TRUE")
                .param("password",passwords.encode(bootstrapPassword)).update();
    }
}
