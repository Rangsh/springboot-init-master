package com.ttk.springbootinit.config.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 文档信息（OpenAPI Info）
 *
 * @author Rangsh
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpringBoot Init Master 接口文档")
                        .description("Spring Boot 3 开发模版接口文档（骨架阶段）")
                        .version("0.0.1")
                        .contact(new Contact().name("Rangsh"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
