package com.justeam.rdp.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebConfiguration implements WebMvcConfigurer {
    private final QueryAuditInterceptor interceptor;
    public AuditWebConfiguration(QueryAuditInterceptor interceptor){this.interceptor=interceptor;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor).addPathPatterns("/api/**");}
}
