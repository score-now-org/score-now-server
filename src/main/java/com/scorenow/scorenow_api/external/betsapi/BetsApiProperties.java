package com.scorenow.scorenow_api.external.betsapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "betsapi")
@Getter
@Setter
public class BetsApiProperties {
	private String baseUrl;
	private String token;
	private int connectTimeout;
	private int readTimeout;
}
