package com.justeam.rdp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.rdp.common.ApiResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableConfigurationProperties(RdpProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper mapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/api/auth/**", "/actuator/health", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/datasets/export-download/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/integrations/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/device-ingest/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(response.getOutputStream(), ApiResponse.error(401, "请先登录"));
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(response.getOutputStream(), ApiResponse.error(403, "无权执行该操作"));
                        }))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(RdpProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(properties.cors().allowedOrigins().split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Signature", "X-Timestamp", "X-Idempotency-Key", "X-Step-Up-Token"));
        config.setExposedHeaders(List.of("Content-Disposition", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
