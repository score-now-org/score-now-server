package com.scorenow.scorenow_api.global.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("⚽ Score Now API Server")
                        .description("Score Now의 백엔드 API 문서입니다.")
                        .version("1.0.0"));
    }
}