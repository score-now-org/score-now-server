package com.scorenow.scorenow_api.global.config;

import com.scorenow.scorenow_api.domain.admin.member.component.AdminLoginIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AdminLoginIntercepter adminLoginIntercepter;

    public WebConfig(AdminLoginIntercepter adminLoginIntercepter) {
        this.adminLoginIntercepter = adminLoginIntercepter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminLoginIntercepter)
                .addPathPatterns("/admin/**");
    }
}
