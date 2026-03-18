package org.example.just.configs;

import org.example.just.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")  // ← 改回这个
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(     // 排除不需要拦截的路径
                        "/user/register",  // 用户注册
                        "/user/login",     // 用户登录
                        "/swagger-ui/**",  // Swagger UI
                        "/swagger-ui.html",// Swagger 页面
                        "/v3/api-docs/**", // OpenAPI 文档
                        "/webjars/**"      // Swagger 静态资源
                );
    }
}




