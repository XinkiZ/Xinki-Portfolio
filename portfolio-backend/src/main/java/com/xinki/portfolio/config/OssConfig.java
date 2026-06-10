package com.xinki.portfolio.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        if (accessKeyId == null || accessKeySecret == null) {
            throw new IllegalStateException(
                    "OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET env vars must be set");
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}