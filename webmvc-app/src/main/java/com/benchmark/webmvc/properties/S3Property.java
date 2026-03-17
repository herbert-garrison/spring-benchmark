package com.benchmark.webmvc.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties("s3")
public class S3Property {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
}
