package com.tensorlab.ml;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Validated
@Component
@ConfigurationProperties(prefix = AppConfig.CONFIG_PREFIX)
public class AppConfig {
	protected static final String CONFIG_PREFIX = "app-config";
	private AwsAuthConfig AwsAuthConfig;

	@Data
	public static class AwsAuthConfig {
		@NotNull
		@ToString.Exclude
		private String accessKey;
		@NotNull
		@ToString.Exclude
		private String secretKey;
		@NotNull
		private String serviceName;
		@NotNull
		private String serviceRegion;
		@NotNull
		private String serviceHost;
		@NotNull
		private String serviceEndPoint;
	}

	@PostConstruct
	private void init() {
		//for debugging purpose, be careful not to print sensitive information in production!
		log.debug("AppConfig loaded: {}", toString());
	}
}
