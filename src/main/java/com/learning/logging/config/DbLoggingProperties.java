package com.learning.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "db.logging")
@Data
public class DbLoggingProperties {
    private boolean enabled = true;
    private boolean logRequestBody = true;
    private boolean logResponseBody = true;
    private int maxBodySize = 10000;
    private List<String> excludePaths = List.of("/actuator", "/swagger", "/v3/api-docs");
    private int retentionDays = 30;

}