package com.scorenow.scorenow_api.global.config.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@RequiredArgsConstructor
@Getter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    private final List<String> allowedOrigins;
}
