package org.example.just.configs;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Just 后端接口文档")
                        .description("用户注册接口文档")
                        .version("1.0.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Wiki"));
    }
}
