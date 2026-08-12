package com.justeam.rdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableAsync
@EnableScheduling
@EnableMethodSecurity
@SpringBootApplication
public class RdpPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(RdpPlatformApplication.class, args);
    }
}
